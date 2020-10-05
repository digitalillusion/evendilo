package xyz.deverse.evendilo.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import xyz.deverse.evendilo.logger
import javax.annotation.PostConstruct

data class WoocommerceCredentials (
    var username: String = "",
    var password: String = ""
)

data class ImporterConfig (
    var imageUploadBaseUrl: String = "",
    var attributes: String = ""
)

open class DestinationConfigurationProperties (
    var identifier: String = "",
    var importerConfig: ImporterConfig = ImporterConfig()
)

data class WoocommerceConfigurationProperties (
    var url: String = "",
    var credentials: WoocommerceCredentials = WoocommerceCredentials()
) : DestinationConfigurationProperties()

data class EbayConfigurationProperties (
        var url: String = "",
        var merchantLocationKey: String = ""
) : DestinationConfigurationProperties()

@Configuration
@ConfigurationProperties(prefix = "variables")
data class AppConfigurationProperties(
    var corsAllowedOrigin: String = "*",
    var woocommerce: MutableList<WoocommerceConfigurationProperties> = mutableListOf(),
    var ebay: MutableList<EbayConfigurationProperties> = mutableListOf(),
    var amazon: MutableList<DestinationConfigurationProperties> = mutableListOf()
) {
    val logger = logger<AppConfigurationProperties>()

    fun woocommerceConfig() : WoocommerceConfigurationProperties {
        var token = SecurityContextHolder.getContext().authentication as OAuth2AuthenticationToken
        for (config in woocommerce) {
            val importerConfig = config.importerConfig;
            if (token.authorizedClientRegistrationId == config.identifier) {
                return config
            }
        }
        throw IllegalAccessException("OAuth2AuthenticationToken.authorizedClientRegistrationId cannot be matched with any Woocommerce configuration: ${token.authorizedClientRegistrationId}")
    }

    fun ebayConfig() : EbayConfigurationProperties {
        var token = SecurityContextHolder.getContext().authentication as OAuth2AuthenticationToken
        for (config in ebay) {
            val importerConfig = config.importerConfig;
            if (token.authorizedClientRegistrationId == config.identifier) {
                return config
            }
        }
        throw IllegalAccessException("OAuth2AuthenticationToken.authorizedClientRegistrationId cannot be matched with any Ebay configuration: ${token.authorizedClientRegistrationId}")
    }

    @PostConstruct
    fun debugAppProperties() {
        logger.debug(this.toString())
    }
}