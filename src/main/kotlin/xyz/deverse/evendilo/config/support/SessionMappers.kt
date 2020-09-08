package xyz.deverse.evendilo.config.support

import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.stereotype.Component
import xyz.deverse.evendilo.model.Destination

interface SessionMapper {
    fun canHandle(domain: String) : Boolean

    fun map(user: DefaultOAuth2User) : MutableMap<String, String>
}

@Component
class WoocommerceSessionMapper : SessionMapper {
    override fun canHandle(domain: String): Boolean {
        return Destination.Woocommerce.name().toLowerCase() == domain
    }

    override fun map(user: DefaultOAuth2User): MutableMap<String, String> {
        return mutableMapOf(
                Pair("email", user.getAttribute("user_email")!!),
                Pair("name", user.getAttribute("user_login")!!)
        )
    }
}

@Component
class EbaySessionMapper : SessionMapper {
    override fun canHandle(domain: String): Boolean {
        return Destination.Ebay.name().toLowerCase() == domain
    }

    override fun map(user: DefaultOAuth2User): MutableMap<String, String> {
        val individualAccount: Map<String, String> = user.getAttribute("individualAccount")!!
        return mutableMapOf(
                Pair("email", "${individualAccount["email"]}"),
                Pair("name", "${individualAccount["firstName"]} ${individualAccount["lastName"]}")
        )
    }
}