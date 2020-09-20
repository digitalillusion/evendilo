package xyz.deverse.evendilo.api.ebay

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpStatus
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
import xyz.deverse.evendilo.logger
import xyz.deverse.evendilo.model.ebay.InventoryLocation
import java.security.InvalidParameterException


class EbayApiCache(
        var restTemplate: RestTemplate
) {
    fun clear() {
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
            var restTemplate: RestTemplate? = null
            for (config in appConfigProperties.ebay) {4
                if (token.authorizedClientRegistrationId == config.identifier) {
                    val client: OAuth2AuthorizedClient = clientService.loadAuthorizedClient(
                            token.authorizedClientRegistrationId,
                            token.name
                    )
                    restTemplate = restTemplateBuilder
                            .rootUri(config.url)
                            .defaultHeader("Authorization", "Bearer " + client.accessToken.tokenValue)
                            .requestFactory(HttpComponentsClientHttpRequestFactory::class.java)
                            .build()
                    break
                }
            }
            if (restTemplate == null) {
                throw InvalidParameterException("OAuth2AuthenticationToken.authorizedClientRegistrationId cannot be matched with any Ebay configuration: ${token.authorizedClientRegistrationId}")
            }
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

    fun ensureInventoryLocation() {
        try {
            retryTemplate.execute<InventoryLocation, Exception> { _ ->
                rest().getForObject("/sell/inventory/v1/location/$EVENDILO_INVENTORY_LOCATION", InventoryLocation::class.java)
            }
        } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.NOT_FOUND -> {
                    var inventoryLocation = InventoryLocation();
                    rest().postForObject("/sell/inventory/v1/location/$EVENDILO_INVENTORY_LOCATION", inventoryLocation, InventoryLocation::class.java)
                }
                else -> {}
            }
        }
    }

}