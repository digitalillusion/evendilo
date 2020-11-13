package xyz.deverse.evendilo.api.ebay

import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.retry.support.RetryTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
import xyz.deverse.evendilo.config.support.LoggingRequestInterceptor
import xyz.deverse.evendilo.functions.getAuthentication
import xyz.deverse.evendilo.logger
import xyz.deverse.evendilo.model.ebay.*
import xyz.deverse.evendilo.model.ebay.EbayConstants.Companion.MARKETPLACE_ID
import java.lang.IllegalStateException


class EbayApiCache(
        var restTemplate: RestTemplate,
        var inventoryLocationCache: HashMap<String, InventoryLocation?>,
        var suggestedCategoryCache: HashMap<String, String>
) {
    constructor(restTemplate: RestTemplate) :
            this(restTemplate, HashMap(), HashMap())

    fun clear() {
        inventoryLocationCache.clear()
        suggestedCategoryCache.clear()
    }
}

@Service
@Scope
class EbayApi(
        var appConfigProperties: AppConfigurationProperties,
        var restTemplateBuilder: RestTemplateBuilder,
        var retryTemplate: RetryTemplate,
        var clientService: OAuth2AuthorizedClientService) {
    val logger = logger<EbayApi>()

    companion object {
        const val EVENDILO_INVENTORY_LOCATION = "evendilo"
    }

    var caches = HashMap<String, EbayApiCache>()


    private fun cache() : EbayApiCache {
        val token = getAuthentication()
        return caches.getOrPut(token.authorizedClientRegistrationId) {
            val factory = HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().build())
            val config = appConfigProperties.ebayConfig();
            val client: OAuth2AuthorizedClient = clientService.loadAuthorizedClient(
                    token.authorizedClientRegistrationId,
                    token.name
            )
            logger.info("Instantiated REST client for ${config.identifier}")
            var restTemplate = restTemplateBuilder
                    .rootUri(config.url)
                    .defaultHeader("Authorization", "Bearer " + client.accessToken.tokenValue)
                    .defaultHeader("Content-Language", EbayConstants.CONTENT_LANGUAGE)
                    .additionalInterceptors(LoggingRequestInterceptor())
                    .requestFactory { BufferingClientHttpRequestFactory(factory) }
                    .build()
            EbayApiCache(restTemplate)
        }
    }

    private fun rest() : RestTemplate {
        return cache().restTemplate
    }

    fun refreshCache() {
        cache().clear()
        logger.info("Cache cleared")
    }

    fun ensureInventoryLocation() : InventoryLocation {
        logger.info("Checking that inventory location $EVENDILO_INVENTORY_LOCATION is existent")
        return cache().inventoryLocationCache.getOrPut(EVENDILO_INVENTORY_LOCATION, {
            try {
                retryTemplate.execute<InventoryLocation, Exception> { _ ->
                    rest().getForObject("/sell/inventory/v1/location/$EVENDILO_INVENTORY_LOCATION", InventoryLocation::class.java)
                }
            } catch (e: HttpClientErrorException) {
                when (e.statusCode) {
                    HttpStatus.NOT_FOUND -> {
                        logger.info("Creating inventory location $EVENDILO_INVENTORY_LOCATION")
                        var inventoryLocation = InventoryLocation();
                        rest().postForObject("/sell/inventory/v1/location/$EVENDILO_INVENTORY_LOCATION", inventoryLocation, InventoryLocation::class.java)
                    }
                    else -> throw e
                }
            }
        })!!
    }

    fun createOrUpdateProduct(product: Product)  {
        logger.info("Creating product ${product.sku} (${product.type})")
        retryTemplate.execute<ResponseEntity<Product>, Exception> {
            var requestEntity = HttpEntity(product)
            rest().exchange("/sell/inventory/v1/inventory_item/${product.sku}", HttpMethod.PUT, requestEntity, Product::class.java)
        }
    }

    fun createOrUpdateOffer(product: Product): Offer {
        logger.info("Creating offer for product ${product.sku}")
        var offer= product.offer
        offer = retryTemplate.execute<Offer, Exception> {
            rest().postForObject("/sell/inventory/v1/offer", offer, Offer::class.java)
        }
        product.offer = offer
        return offer
    }

    fun publishOffer(offer: Offer) {
        retryTemplate.execute<Void, Exception> {
            rest().postForObject("/sell/inventory/v1/offer/${offer.id}/publish", null, Void::class.java)
        }
    }

    fun getCategorySuggestions(productInfo: ProductInfo): String {
        logger.info("Retrieving category suggestion")
        return cache().suggestedCategoryCache.getOrPut(productInfo.title) {
            val categoryTreeId = retryTemplate.execute<Map<*, *>, Exception> {
                rest().getForObject("/commerce/taxonomy/v1/get_default_category_tree_id?marketplace_id=${MARKETPLACE_ID}", Map::class.java)
            }["categoryTreeId"]!!;

            val categories: List<Map<String, String>> = retryTemplate.execute<Map<*, *>, Exception> {
                rest().getForObject("/commerce/taxonomy/v1/category_tree/${categoryTreeId}/get_category_suggestions?q=${productInfo.title}", Map::class.java)
            }["categorySuggestions"] as List<Map<String, String>>
            val categoryId: String = (categories[0]["category"] as Map<*, *>)["categoryId"] as String
            categoryId
        }
    }

    fun findProduct(product: Product): Product? {
        try {
            var responseProduct = retryTemplate.execute<Product?, Exception> {
                rest().getForObject("/sell/inventory/v1/inventory_item/${product.sku}", Product::class.java)
            }
            responseProduct?.type = product.type

            findOffer(product)?.let { responseProduct?.offer?.from(it) }
            return responseProduct
        } catch (e: HttpClientErrorException) {
            product.id = null
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return null
            }
            throw e
        }
    }

    private fun findOffer(product: Product): Offer? {
        try {
            return retryTemplate.execute<Offer?, Exception> {
                rest().getForObject("/sell/inventory/v1/offer?sku=${product.sku}", Offer::class.java)
            }
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return null
            }
            throw e
        }
    }

    fun createGroup(product: Product, skus: List<String>): Group {
        val group = Group(product, skus.toMutableList())
        retryTemplate.execute<ResponseEntity<Group>, Exception> {
            val requestEntity = HttpEntity(group)
            rest().exchange("/sell/inventory/v1/inventory_item_group/${product.sku}", HttpMethod.PUT, requestEntity, Group::class.java)
        }
        return group
    }

    fun publishOfferForGroup(product: Product, group: Group) {
        retryTemplate.execute<Map<*, *>, Exception> {
            val request = mapOf(
                "inventoryItemGroupKey" to group.inventoryItemGroupKey!!,
                "marketplaceId" to product.offer.marketplaceId
            )
            rest().postForObject("/sell/inventory/v1/offer/publish_by_inventory_item_group", request, Map::class.java)
        }
    }

}