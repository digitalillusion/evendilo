package xyz.deverse.evendilo.model

import com.fasterxml.jackson.annotation.JsonProperty
import xyz.deverse.importer.generic.ImportTag

sealed class Family(private val type: String) : ImportTag {
    @JsonProperty
    override fun name(): String {
        return this.javaClass.simpleName;
    }

    @JsonProperty("type")
    override fun toString(): String {
        return type
    }

    companion object {
        fun valueOf(type: String): Family {
            return when (type) {
                Family.Standard.toString() -> Family.Standard
                else -> throw IllegalArgumentException(type)
            }
        }
    }

    object Standard : Family("standard")
}