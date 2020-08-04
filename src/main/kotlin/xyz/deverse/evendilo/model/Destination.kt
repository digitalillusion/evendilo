package xyz.deverse.evendilo.model

import com.fasterxml.jackson.annotation.JsonProperty
import xyz.deverse.importer.generic.ImportTag

sealed class Destination(private val type: String) : ImportTag {
    @JsonProperty
    override fun name(): String {
        return this.javaClass.simpleName;
    }

    @JsonProperty("type")
    override fun toString(): String {
        return type
    }

    companion object {
        fun valueOf(type: String): Destination {
            return when (type) {
                WooCommerce.toString() -> WooCommerce
                else -> throw IllegalArgumentException(type)
            }
        }
    }

    object WooCommerce : Destination("woocommerce");
}