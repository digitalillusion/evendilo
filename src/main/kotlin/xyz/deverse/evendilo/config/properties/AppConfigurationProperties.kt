package xyz.deverse.evendilo.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import xyz.deverse.evendilo.logger
import javax.annotation.PostConstruct

data class WoocommerceCredentials (
    var username: String = "",
    var password: String = ""
)

data class ImporterConfig (
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

    @PostConstruct
    fun debugAppProperties() {
        logger.debug(this.toString())
    }
}