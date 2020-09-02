package xyz.deverse.evendilo.api.ebay

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
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

class EbayApiCache (
    var restTemplate: RestTemplate
) {
    fun clear() {
    }
}

@Service
@Scope
class EbayApi(var appConfigProperties: AppConfigurationProperties, var restTemplateBuilder: RestTemplateBuilder, var retryTemplate: RetryTemplate) {
    val logger = logger<EbayApi>()

    var caches = HashMap<String, EbayApiCache>()

    private fun cache() : EbayApiCache {
        var token = SecurityContextHolder.getContext().authentication as OAuth2AuthenticationToken
        return caches.getOrPut(token.authorizedClientRegistrationId) {
            var restTemplate: RestTemplate? = null
            for (config in appConfigProperties.woocommerce) {
                val credentials = config.credentials;
                if (token.authorizedClientRegistrationId == config.identifier) {
                    logger.info("Instantiated REST client for ${config.identifier}")
                    restTemplate = restTemplateBuilder
                            .rootUri(config.url)
                            .basicAuthentication(credentials.username, credentials.password)
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

}