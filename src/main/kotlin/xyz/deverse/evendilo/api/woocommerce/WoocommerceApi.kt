package xyz.deverse.evendilo.api.woocommerce

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
import xyz.deverse.evendilo.config.support.LoggingRequestInterceptor
import xyz.deverse.evendilo.functions.getAuthentication
import xyz.deverse.evendilo.functions.replaceList
import xyz.deverse.evendilo.logger
import xyz.deverse.evendilo.model.woocommerce.*


const val MAX_PRODUCT_CACHE_SIZE : Int = 3


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
        var categoryCache: LinkedHashMap<String, Array<Category>?>,
        var tagsCache: LinkedHashMap<String, Array<Tag>?>,
        var productCache: LinkedHashMap<String, Array<Product>?>,
        var attributeCache: MutableList<Attribute>,
        var attributeTermCache: LinkedHashMap<String, Array<AttributeTerm>>
) {
    constructor(restTemplate: RestTemplate) :
            this(restTemplate, LinkedHashMap(), LinkedHashMap(), object : LinkedHashMap<String, Array<Product>?>() {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Array<Product>?>): Boolean {
                    return this.size > MAX_PRODUCT_CACHE_SIZE
                }
            }, mutableListOf(), LinkedHashMap())

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
        val token = getAuthentication()
        return caches.getOrPut(token.authorizedClientRegistrationId) {
            val factory = HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().build())
            val config = appConfigProperties.woocommerceConfig()
            val credentials = config.credentials;
            logger.info("Instantiated REST client for ${config.identifier}")
            var restTemplate = restTemplateBuilder
                    .rootUri(config.url)
                    .basicAuthentication(credentials.username, credentials.password)
                    .requestFactory { BufferingClientHttpRequestFactory(factory) }
                    .additionalInterceptors(LoggingRequestInterceptor())
                    .build()
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
                        val response = retryTemplate.execute<Array<Tag>, Exception> { 
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

    fun findCategoriesByName(search: String) : List<Category> {
        val categories = cache().categoryCache
                .getOrPut(search)  {
                    logger.info("Searching categories $search")
                    var categories = mutableListOf<Category>()
                    var page = 1
                    do {
                        val response = retryTemplate.execute<Array<Category>, Exception> {
                            rest().getForObject("/wp-json/wc/v3/products/categories?search={search}&per_page=100&page={page}", Array<Category>::class.java, search, page)!!
                        }
                        response.forEach { categories.add(it) }
                        page++
                    } while (response.isNotEmpty())
                    categories.toTypedArray()
                }

        return categories?.asList() ?: listOf()
    }

    fun findCategory(search: Category?) : Category? {
        val categories = findCategoriesByName("")
        return if (categories.isNotEmpty()) categories.find { it.name == search?.name ?: true } else null
    }

    fun createProduct(product: Product) : Product {
        if (product.id != null) {
            return product
        }
        convertAttributeTermsToNames(product.attributes)
        logger.info("Creating product ${product.name}:")
        val responseProduct = retryTemplate.execute<Product, Exception> { 
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
        logger.info("Updating product ${product.name} (${product.id}):")
        val response: ResponseEntity<Product> = retryTemplate.execute<ResponseEntity<Product>, Exception> { 
            rest().exchange("/wp-json/wc/v3/products/{productId}", HttpMethod.PUT, requestEntity, product.id!!)
        }
        val responseProduct = response.body!!

        responseProduct.categories = product.categories // Categories are not updated in the response

        cache().productCache[responseProduct.name] = arrayOf(responseProduct)
        return responseProduct
    }

    fun findProduct(product: Product) : Product? {
        val search = product.name
        val sku = product.sku
        val products = cache().productCache
                .getOrPut(search)  {
                    logger.info("Searching product $search (sku=$sku)")
                    retryTemplate.execute<Array<Product>, Exception> {
                        rest().getForObject("/wp-json/wc/v3/products?sku={sku}", Array<Product>::class.java, sku)
                    }
                }

        if (products == null || products.isEmpty()) {
            return null
        } else if (products.size == 1) {
            if (products[0].name != search) {
                logger.warn("Renaming product ${products[0].name} to $search (sku=$sku)")
            }
            return products[0]
        }
        throw IllegalStateException("Obtained a count of ${products.size} products for name=${product.name}: ${products.map { it.id }.joinToString(", ")}")
    }

    fun findProductVariation(product: Product, variation: ProductVariation): ProductVariation? {
        val sku = variation.sku
        val variations = retryTemplate.execute<Array<ProductVariation>, Exception> { 
            rest().getForObject("/wp-json/wc/v3/products/{productId}/variations?sku={sku}", Array<ProductVariation>::class.java, product.id, sku)
        }

        if (variations == null || variations.isEmpty()) {
            return null
        } else if (variations.size == 1) {
            return variations[0]
        }
        throw IllegalStateException("Obtained a count of ${variations.size} product varationss for sku=${product.sku}: ${variations.map { it.id }.joinToString(", ")}")
    }

    fun findAttribute(attribute: Attribute) : Attribute? {
        if (attribute.name.isBlank() || attribute.asSingle().option?.asName()?.name?.isBlank() != false) {
            return null
        }
        if (cache().attributeCache.isEmpty() || !cache().attributeCache.any() { it.name == attribute.name }) {
            cache().attributeCache.clear()
            logger.info("Searching attributes")
            val attributes = retryTemplate.execute<Array<Attribute>, Exception> { 
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
                        val response = retryTemplate.execute<Array<AttributeTerm>, Exception> { 
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
            result = retryTemplate.execute<Attribute, Exception> { 
                rest().postForObject("/wp-json/wc/v3/products/attributes", attribute, Attribute::class.java)!!
            }
        }

        var resultOptions = mutableListOf<AttributeTerm>()
        for (option in attribute.options) {
            if (option.id == null && option::class != AttributeTerm.Name::class && !option.name.isBlank()) {
                logger.info("Creating attribute ${attribute.name} with option ${option.name}")
                val responseTerm = retryTemplate.execute<AttributeTerm, Exception> { 
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
                    val responseTerm = retryTemplate.execute<AttributeTerm, Exception> {
                        try {
                            rest().postForObject("/wp-json/wc/v3/products/attributes/{attributeId}/terms", term, AttributeTerm::class.java, attribute.id)!!
                        } catch (e: HttpClientErrorException.BadRequest) {
                            val mapper = ObjectMapper()
                            val errorResponse: JsonNode = mapper.readTree(e.responseBodyAsString)
                            if (errorResponse.get("code").asText() == "term_exists") {
                                val termId = errorResponse.get("data").get("resource_id").asLong()
                                logger.info("Attribute ${attribute.name} terms option ${option.name} exists with id $termId")
                                AttributeTerm.NewTerm(termId, term.name)
                            } else {
                                throw e
                            }
                        }
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
            logger.info("Creating product ${product.name} first variation ${variation.sku}:")
        } else {
            logger.info("Creating product ${product.name} (${product.id}) variation ${variation.sku}:")
        }
        convertAttributeTermsToNames(variation.attributes)
        convertAttributesToSingle(variation)
        val responseVariation = retryTemplate.execute<ProductVariation, Exception> { 
            rest().postForObject("/wp-json/wc/v3/products/{productId}/variations", variation, ProductVariation::class.java, product.id)!!
        }
        product.images.add(responseVariation.image)
        product.images = product.images.distinctBy { image -> image.id }.toMutableList()

        cache().productCache[product.name]!![0].variations.replaceAll { v -> if (v.sku == responseVariation.sku) { responseVariation } else { v } }
        return responseVariation
    }

    fun updateProductVariation(product: Product, variation: ProductVariation) : ProductVariation {
        logger.info("Updating product ${product.name} (${product.id}) variation ${variation.sku}:")
        convertAttributeTermsToNames(variation.attributes)
        convertAttributesToSingle(variation)
        val response = retryTemplate.execute<ResponseEntity<ProductVariation>, Exception> { 
            val requestEntity: HttpEntity<ProductVariation> = HttpEntity(variation)
            rest().exchange("/wp-json/wc/v3/products/{productId}/variations/{variationId}", HttpMethod.PUT, requestEntity, product.id!!, variation.id!!)
        }
        val responseVariation = response.body!!

        cache().productCache[product.name]!![0].variations.replaceAll { v -> if (v.sku == responseVariation.sku) { responseVariation } else { v } }
        return responseVariation
    }

}