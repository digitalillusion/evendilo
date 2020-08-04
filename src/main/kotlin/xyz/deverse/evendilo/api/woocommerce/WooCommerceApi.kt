package xyz.deverse.evendilo.api.woocommerce

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
import xyz.deverse.evendilo.model.woocommerce.Category
import xyz.deverse.evendilo.model.woocommerce.Product

@Service
class WooCommerceApi(var appConfigProperties: AppConfigurationProperties, var restTemplatesBuilder: RestTemplateBuilder) {

    val categoryCache = HashMap<String, Array<Category>?>()

    private fun rest() : RestTemplate {
        for (config in appConfigProperties.woocommerce) {
            val credentials = config.credentials;
            // TODO find the correct one according to the config.identifier
            return restTemplatesBuilder
                    .rootUri(config.url)
                    .basicAuthentication(credentials.username, credentials.password)
                    .build()
        }
        return restTemplatesBuilder.build()
    }

    fun refreshCache() {
        categoryCache.clear()
    }

    fun findCategories(search: String = "", exact: Boolean) : List<Category> {
        val categories = categoryCache
                .getOrPut(search)  { rest().getForObject("/wp-json/wc/v3/products/categories?search={search}", Array<Category>::class.java, search) }

        if (search.isEmpty() || categories == null || !exact) {
            return categories?.asList() ?: listOf()
        }

        val exactMatch = categories.find { it.name == search }
        return if (exactMatch != null) { listOf(exactMatch) } else { listOf() }
    }

    fun findCategory(search: String = "") : Category? {
        val categories = findCategories(search, true)
        return if (categories.isNotEmpty()) categories[0] else null
    }

    fun createProduct(product: Product) : Product {
        return rest().postForObject("/wp-json/wc/v3/products", product, Product::class.java)!!
    }
}