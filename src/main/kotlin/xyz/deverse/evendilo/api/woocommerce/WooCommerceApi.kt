package xyz.deverse.evendilo.api.woocommerce

import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
import xyz.deverse.evendilo.functions.replaceList
import xyz.deverse.evendilo.model.woocommerce.*
import java.security.InvalidParameterException


fun convertAttributeTermsToNames(attributes: MutableList<Attribute>) {
    for (attribute in attributes) {
        replaceList(attribute.options) {
            when {
                it::class == AttributeTerm.Term::class -> {
                    val asName = (it as AttributeTerm.Term).asName()
                    asName
                }
                it::class == AttributeTerm.NewTerm::class -> {
                    val asName = (it as AttributeTerm.NewTerm).asName()
                    asName
                }
                else -> {
                    it
                }
            }
        }
    }
}

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class WooCommerceApi(var appConfigProperties: AppConfigurationProperties, var restTemplateBuilder: RestTemplateBuilder) {

    val categoryCache = HashMap<String, Array<Category>?>()
    val restTemplateCache = HashMap<String, RestTemplate>()
    val attributeCache = mutableListOf<Attribute>()

    private fun rest() : RestTemplate {
        var token = SecurityContextHolder.getContext().authentication as OAuth2AuthenticationToken
        return restTemplateCache.getOrPut(token.authorizedClientRegistrationId) {
            for (config in appConfigProperties.woocommerce) {
                val credentials = config.credentials;
                if (token.authorizedClientRegistrationId == config.identifier) {
                    return restTemplateBuilder
                            .rootUri(config.url)
                            .basicAuthentication(credentials.username, credentials.password)
                            .requestFactory(HttpComponentsClientHttpRequestFactory::class.java)
                            .build()
                }
            }
            throw InvalidParameterException("OAuth2AuthenticationToken.authorizedClientRegistrationId cannot be matched with any WooCommerce configuration: ${token.authorizedClientRegistrationId }")
        }
    }

    fun refreshCache() {
        restTemplateCache.clear()
        categoryCache.clear()
        attributeCache.clear()
    }

    fun findCategoriesByName(search: String, exact: Boolean) : List<Category> {
        val categories = categoryCache
                .getOrPut(search)  { rest().getForObject("/wp-json/wc/v3/products/categories?search={search}", Array<Category>::class.java, search) }

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
        return rest().postForObject("/wp-json/wc/v3/products", product, Product::class.java)!!
    }

    fun updateProduct(product: Product) : Product {
        if (product.id == null) {
            throw IllegalStateException("The product has no id so it must be created instead of updated")
        }
        convertAttributeTermsToNames(product.attributes)
        val requestEntity: HttpEntity<Product> = HttpEntity(product)
        val response: ResponseEntity<Product> = rest().exchange("/wp-json/wc/v3/products/{productId}", HttpMethod.PUT, requestEntity, product.id!!)
        return response.body!!
    }

    fun findProduct(product: Product) : Product? {
        val products = rest().getForObject("/wp-json/wc/v3/products?search=${product.name}", Array<Product>::class.java)
        if (products == null || products.isEmpty()) {
            return null
        } else if (products.size == 1) {
            return products[0]
        }
        throw IllegalStateException("Obtained a count of ${products.size} products for name=${product.name}: ${products.map { it.id }.joinToString(", ")}")
    }


    fun findAttribute(attribute: Attribute) : Attribute? {
        if (attributeCache.isEmpty() || !attributeCache.any() { it.name == attribute.name }) {
            attributeCache.clear()
            val attributes = rest().getForObject("/wp-json/wc/v3/products/attributes", Array<Attribute>::class.java)
            attributes?.forEach { attributeCache.add(it) }
        }
        return attributeCache.find { it.name == attribute.name }
    }

    fun findAttributeTerms(attribute: Attribute) : MutableList<AttributeTerm> {
        return rest().getForObject("/wp-json/wc/v3/products/attributes/{attributeId}/terms", Array<AttributeTerm>::class.java, attribute.id)!!.toMutableList()
    }

    fun createAttribute(attribute: Attribute): Attribute {
        var result = attribute
        if (attribute.id == null) {
            result = rest().postForObject("/wp-json/wc/v3/products/attributes", attribute, Attribute::class.java)!!
        }

        var resultOptions = mutableListOf<AttributeTerm>()
        for (option in attribute.options) {
            if (option.id == null && option::class != AttributeTerm.Name::class && !option.name.isBlank()) {
                resultOptions.add(rest().postForObject("/wp-json/wc/v3/products/attributes/{attributeId}/terms", option, AttributeTerm::class.java, result.id)!!)
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
                    rest().postForObject("/wp-json/wc/v3/products/attributes/{attributeId}/terms", term, AttributeTerm::class.java, attribute.id)
                }
            }
        }
        return attribute
    }

    fun createProductVariation(product: Product, variation: ProductVariation) : ProductVariation {
        if (variation.id != null) {
            return variation
        }
        convertAttributeTermsToNames(variation.attributes)
        return rest().postForObject("/wp-json/wc/v3/products/{productId}/variations", variation, ProductVariation::class.java, product.id)!!
    }
}