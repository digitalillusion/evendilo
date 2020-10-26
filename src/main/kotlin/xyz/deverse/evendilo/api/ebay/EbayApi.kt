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
import xyz.deverse.evendilo.logger
import xyz.deverse.evendilo.model.ebay.*
import xyz.deverse.evendilo.model.ebay.EbayConstants.Companion.MARKETPLACE_ID


class EbayApiCache(
        var restTemplate: RestTemplate,
        var inventoryLocationCache: HashMap<String, InventoryLocation?>
) {
    constructor(restTemplate: RestTemplate) :
            this(restTemplate, HashMap())

    fun clear() {
        inventoryLocationCache.clear()
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
        var token = SecurityContextHolder.getContext().authentication as OAuth2AuthenticationToken
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

    fun createProduct(product: Product)  {
        logger.info("Creating product ${product.product.title}")
        retryTemplate.execute<ResponseEntity<Product>, Exception> {
            var requestEntity = HttpEntity(product)
            rest().exchange("/sell/inventory/v1/inventory_item/${product.sku}", HttpMethod.PUT, requestEntity, Product::class.java)
        }
    }

    fun createOffer(product: Product) {
        logger.info("Creating offer for product ${product.product.title}")
        var offer= product.offer
        offer = retryTemplate.execute<Offer, Exception> {
            rest().postForObject("/sell/inventory/v1/offer", offer, Offer::class.java)
        }

        retryTemplate.execute<Void, Exception> {
            rest().postForObject("/sell/inventory/v1/offer/${offer.id}/publish", null, Void::class.java)
        }
    }

    fun getCategorySuggestions(productInfo: ProductInfo): String {
        logger.info("Retrieving category suggestions")
        var categoryTreeId = retryTemplate.execute<Map<*, *>, Exception> {
            rest().getForObject("/commerce/taxonomy/v1/get_default_category_tree_id?marketplace_id=${MARKETPLACE_ID}", Map::class.java)
        }["categoryTreeId"]!!;

        var categories: List<Map<String, String>> = retryTemplate.execute<Map<*, *>, Exception> {
            rest().getForObject("/commerce/taxonomy/v1/category_tree/${categoryTreeId}/get_category_suggestions?q=${productInfo.title}", Map::class.java)
        }["categorySuggestions"] as List<Map<String, String>>
        var categoryId: String = (categories[0]["category"] as Map<*, *>)["categoryId"] as String;
        return categoryId
    }

}