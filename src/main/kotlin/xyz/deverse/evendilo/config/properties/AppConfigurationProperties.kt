package xyz.deverse.evendilo.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import xyz.deverse.evendilo.importer.business.EntityFactory
import xyz.deverse.evendilo.logger
import javax.annotation.PostConstruct

data class WooCommerceCredentials (
    var username: String = "",
    var password: String = ""
)

data class WooCommerceConfigurationProperties (
    var identifier: String = "",
    var url: String = "",
    var credentials: WooCommerceCredentials = WooCommerceCredentials()
)

@Configuration
@ConfigurationProperties(prefix = "variables")
data class AppConfigurationProperties(
    var corsAllowedOrigin: String = "*",
    var woocommerce: MutableList<WooCommerceConfigurationProperties> = mutableListOf()
) {
    val logger = logger<AppConfigurationProperties>()

    @PostConstruct
    fun debugAppProperties() {
        logger.info(this.toString())
    }
}