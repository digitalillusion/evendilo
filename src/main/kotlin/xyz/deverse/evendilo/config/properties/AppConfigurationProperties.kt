package xyz.deverse.evendilo.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import xyz.deverse.evendilo.logger
import javax.annotation.PostConstruct

data class WoocommerceCredentials (
    var username: String = "",
    var password: String = ""
)

data class WoocommerceImporterConfig (
    var attributes: String = ""
)

data class WoocommerceConfigurationProperties (
    var identifier: String = "",
    var url: String = "",
    var importerConfig: WoocommerceImporterConfig = WoocommerceImporterConfig(),
    var credentials: WoocommerceCredentials = WoocommerceCredentials()
)

data class EbayConfigurationProperties (
        var identifier: String = "",
        var url: String = "",
        var merchantLocationKey: String = ""
)

@Configuration
@ConfigurationProperties(prefix = "variables")
data class AppConfigurationProperties(
    var corsAllowedOrigin: String = "*",
    var woocommerce: MutableList<WoocommerceConfigurationProperties> = mutableListOf(),
    var ebay: MutableList<EbayConfigurationProperties> = mutableListOf()
) {
    val logger = logger<AppConfigurationProperties>()

    @PostConstruct
    fun debugAppProperties() {
        logger.debug(this.toString())
    }
}