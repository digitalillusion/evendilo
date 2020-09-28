package xyz.deverse.evendilo.api.woocommerce

import xyz.deverse.evendilo.config.support.LoggingRequestInterceptor
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.retry.support.RetryTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
import xyz.deverse.evendilo.functions.replaceList
import xyz.deverse.evendilo.logger
import xyz.deverse.evendilo.model.woocommerce.*
import java.security.InvalidParameterException


fun convertAttributesToSingle(variation: ProductVariation) {
    replaceList(variation.attributes) { it.asSingle() }
}

fun convertAttributeTermsToNames(attributes: MutableList<Attribute>) {
    for (attribute in attributes) {
        replaceList(attribute.options) { it.asName() }
    }
}

class WoocommerceApiCache(
        var restTemplate: RestTemplate,
        var categoryCache: HashMap<String, Array<Category>?>,
        var tagsCache: HashMap<String, Array<Tag>?>,
        var productCache: HashMap<String, Array<Product>?>,
        var attributeCache: MutableList<Attribute>,
        var attributeTermCache: HashMap<String, Array<AttributeTerm>>
) {
    constructor(restTemplate: RestTemplate) :
            this(restTemplate, HashMap(), HashMap(), HashMap(), mutableListOf(), HashMap())

    fun clear() {
        categoryCache.clear()
        tagsCache.clear()
        attributeCache.clear()
        productCache.clear()
    }
}

@Service
@Scope
class WoocommerceApi(var appConfigProperties: AppConfigurationProperties, var restTemplateBuilder: RestTemplateBuilder, var retryTemplate: RetryTemplate) {
    val logger = logger<WoocommerceApi>()

    var caches = HashMap<String, WoocommerceApiCache>()

    private fun cache() : WoocommerceApiCache {
        var token = SecurityContextHolder.getContext().authentication as OAuth2AuthenticationToken
        return caches.getOrPut(token.authorizedClientRegistrationId) {
            var restTemplate: RestTemplate? = null
            val factory = HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().build())
            for (config in appConfigProperties.woocommerce) {
                val credentials = config.credentials;
                if (token.authorizedClientRegistrationId == config.identifier) {
                    logger.info("Instantiated REST client for ${config.identifier}")
                    restTemplate = restTemplateBuilder
                            .rootUri(config.url)
                            .basicAuthentication(credentials.username, credentials.password)
                            .requestFactory { BufferingClientHttpRequestFactory(factory) }
                            .additionalInterceptors(LoggingRequestInterceptor())
                            .build()
                    break
                }
            }
            if (restTemplate == null) {
                throw InvalidParameterException("OAuth2AuthenticationToken.authorizedClientRegistrationId cannot be matched with any Woocommerce configuration: ${token.authorizedClientRegistrationId}")
            }
            WoocommerceApiCache(restTemplate)
        }
    }

    private fun rest() : RestTemplate {
        return cache().restTemplate
    }

    fun refreshCache() {
        cache().clear()
        logger.info("Cache cleared")
    }

    fun findTagsByName(search: String, exact: Boolean) : List<Tag> {
        val tags = cache().tagsCache
                .getOrPut(search)  {
                    logger.info("Searching tag $search (exact=$exact)")
                    var tags = mutableListOf<Tag>()
                    var page = 1
                    do {
                        val response = retryTemplate.execute<Array<Tag>, Exception> { _ ->
                            rest().getForObject("/wp-json/wc/v3/products/tags?search={search}&per_page=100&page={page}", Array<Tag>::class.java, search, page)!!
                        }
                        response.forEach { tags.add(it) }
                        page++
                    } while (response.isNotEmpty())
                    tags.toTypedArray()
                }

        if (tags == null || !exact) {
            return tags?.asList() ?: listOf()
        }

        val exactMatch = tags.find { it.name == search }
        return if (exactMatch != null) { listOf(exactMatch) } else { listOf() }
    }

    fun findTag(search: Tag?) : Tag? {
        val tags = findTagsByName(search?.name ?: "", true)
        return if (tags.isNotEmpty()) tags[0] else null
    }

    fun findCategoriesByName(search: String, exact: Boolean) : List<Category> {
        val categories = cache().categoryCache
                .getOrPut(search)  {
                    logger.info("Searching category $search (exact=$exact)")
                    var categories = mutableListOf<Category>()
                    var page = 1
                    do {
                        val response = retryTemplate.execute<Array<Category>, Exception> { _ ->
                            rest().getForObject("/wp-json/wc/v3/products/categories?search={search}&per_page=100&page={page}", Array<Category>::class.java, search, page)!!
                        }
                        response.forEach { categories.add(it) }
                        page++
                    } while (response.isNotEmpty())
                    categories.toTypedArray()
                }

        if (categories == null || !exact) {
            return categories?.asList() ?: listOf()
        }

        val exactMatch = categories.find { it.name == search }
        return if (exactMatch != null) { listOf(exactMatch) } else { listOf() }
    }

    fun findCategory(search: Category?) : Category? {
        val categories = findCategoriesByName(search?.name ?: "", true)
        return if (categories.isNotEmpty()) categories[0] else null
    }

    fun createProduct(product: Product) : Product {
        if (product.id != null) {
            return product
        }
        convertAttributeTermsToNames(product.attributes)
        logger.info("Creating product ${product.name}")
        val responseProduct = retryTemplate.execute<Product, Exception> { _ ->
            rest().postForObject("/wp-json/wc/v3/products", product, Product::class.java)!!
        }
        cache().productCache[responseProduct.name] = arrayOf(responseProduct)
        return responseProduct
    }

    fun updateProduct(product: Product) : Product {
        if (product.id == null) {
            throw IllegalStateException("The product has no id so it must be created instead of updated")
        }
        convertAttributeTermsToNames(product.attributes)
        val requestEntity: HttpEntity<Product> = HttpEntity(product)
        logger.info("Updating product ${product.name} (${product.id})")
        val response: ResponseEntity<Product> = retryTemplate.execute<ResponseEntity<Product>, Exception> { _ ->
            rest().exchange("/wp-json/wc/v3/products/{productId}", HttpMethod.PUT, requestEntity, product.id!!)
        }
        val responseProduct = response.body!!
        cache().productCache[responseProduct.name] = arrayOf(responseProduct)
        return responseProduct
    }

    fun findProduct(product: Product) : Product? {
        val search = product.name
        val type = product.type
        val sku = product.sku
        val products = cache().productCache
                .getOrPut(search)  {
                    logger.info("Searching product $search (type=$type, sku=$sku)")
                    retryTemplate.execute<Array<Product>, Exception> { _ ->
                        rest().getForObject("/wp-json/wc/v3/products?search={search}&type={type}&sku={sku}", Array<Product>::class.java, search, type, sku)
                    }
                }

        if (products == null || products.isEmpty()) {
            return null
        } else if (products.size == 1) {
            return products[0]
        }
        throw IllegalStateException("Obtained a count of ${products.size} products for name=${product.name}: ${products.map { it.id }.joinToString(", ")}")
    }


    fun findAttribute(attribute: Attribute) : Attribute? {
        if (cache().attributeCache.isEmpty() || !cache().attributeCache.any() { it.name == attribute.name }) {
            cache().attributeCache.clear()
            logger.info("Searching attributes")
            val attributes = retryTemplate.execute<Array<Attribute>, Exception> { _ ->
                rest().getForObject("/wp-json/wc/v3/products/attributes", Array<Attribute>::class.java)
            }
            attributes?.forEach { cache().attributeCache.add(it) }
        }
        return cache().attributeCache.find { it.name == attribute.name }
    }

    fun findAttributeTerms(attribute: Attribute) : MutableList<AttributeTerm> {
        val attributeTerms = cache().attributeTermCache
                .getOrPut(attribute.name)  {
                    logger.info("Searching attribute ${attribute.name} terms")
                    var terms = mutableListOf<AttributeTerm>()
                    var page = 1
                    do {
                        val response = retryTemplate.execute<Array<AttributeTerm>, Exception> { _ ->
                            rest().getForObject("/wp-json/wc/v3/products/attributes/{attributeId}/terms?search=&per_page=100&page={page}", Array<AttributeTerm>::class.java, attribute.id, page)!!
                        }
                        response.forEach { terms.add(it) }
                        page++
                    } while (response.isNotEmpty())
                    terms.toTypedArray()
                }
        return attributeTerms.toMutableList()
    }

    fun createAttribute(attribute: Attribute): Attribute {
        var result = attribute
        if (attribute.id == null) {
            logger.info("Creating attribute ${attribute.name}")
            result = retryTemplate.execute<Attribute, Exception> { _ ->
                rest().postForObject("/wp-json/wc/v3/products/attributes", attribute, Attribute::class.java)!!
            }
        }

        var resultOptions = mutableListOf<AttributeTerm>()
        for (option in attribute.options) {
            if (option.id == null && option::class != AttributeTerm.Name::class && !option.name.isBlank()) {
                logger.info("Creating attribute ${attribute.name} with option ${option.name}")
                val responseTerm = retryTemplate.execute<AttributeTerm, Exception> { _ ->
                    rest().postForObject("/wp-json/wc/v3/products/attributes/{attributeId}/terms", option, AttributeTerm::class.java, result.id)!!
                }
                resultOptions.add(responseTerm)
                val cachedTerms = cache().attributeTermCache[attribute.name]?.toMutableList() ?: mutableListOf()
                cachedTerms.add(responseTerm)
                cache().attributeTermCache[attribute.name] = cachedTerms.toTypedArray()
            } else {
                resultOptions.add(option)
            }
        }
        result.options = resultOptions
        return result
    }

    fun updateAttribute(attribute: Attribute, optionsToAdd: MutableList<AttributeTerm>): Attribute {
        val attributeTerms = findAttributeTerms(attribute)
        val sentTerms = mutableListOf<String>()
        for (option in optionsToAdd) {
            val trimmedName2 = option.name.toLowerCase().replace("\\s+".toRegex(), "")
            if (attributeTerms.none {
                    val trimmedName1 = it.name.toLowerCase().replace("\\s+".toRegex(), "")
                    trimmedName1 == trimmedName2
                }) {
                if (!sentTerms.contains(trimmedName2)) {
                    sentTerms.add(trimmedName2)
                    val term = AttributeTerm.NewTerm(null, option.name)
                    logger.info("Updating attribute ${attribute.name} terms, creating option ${option.name}")
                    val responseTerm = retryTemplate.execute<AttributeTerm, Exception> { _ ->
                        rest().postForObject("/wp-json/wc/v3/products/attributes/{attributeId}/terms", term, AttributeTerm::class.java, attribute.id)!!
                    }
                    val cachedTerms = cache().attributeTermCache[attribute.name]?.toMutableList() ?: mutableListOf()
                    cachedTerms.add(responseTerm)
                    cache().attributeTermCache[attribute.name] = cachedTerms.toTypedArray()
                }
            }
        }
        return attribute
    }

    fun createProductVariation(product: Product, variation: ProductVariation) : ProductVariation {
        if (variation.id != null) {
            return variation
        }
        if (product.id == null) {
            logger.info("Creating product ${product.name} first variation ${variation.sku}")
        } else {
            logger.info("Creating product ${product.name} (${product.id}) variation ${variation.sku}")
        }
        convertAttributeTermsToNames(variation.attributes)
        convertAttributesToSingle(variation)
        val responseVariation = retryTemplate.execute<ProductVariation, Exception> { _ ->
            rest().postForObject("/wp-json/wc/v3/products/{productId}/variations", variation, ProductVariation::class.java, product.id)!!
        }
        product.images.add(responseVariation.image)

        cache().productCache[product.name]!![0].variations.replaceAll { v -> if (v.sku == responseVariation.sku) { responseVariation } else { v } }
        return responseVariation
    }

}