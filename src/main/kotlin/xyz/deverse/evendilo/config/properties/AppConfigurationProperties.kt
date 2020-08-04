package xyz.deverse.evendilo.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

class WooCommerceCredentials (
    var username: String = "",
    var password: String = ""
)

class WooCommerceConfigurationProperties (
    var identifier: String = "",
    var url: String = "",
    var credentials: WooCommerceCredentials = WooCommerceCredentials()
)

@Configuration
@ConfigurationProperties(prefix = "variables")
class AppConfigurationProperties() {
    var corsAllowedOrigin: String = "*"
    var woocommerce: MutableList<WooCommerceConfigurationProperties> = mutableListOf()
}