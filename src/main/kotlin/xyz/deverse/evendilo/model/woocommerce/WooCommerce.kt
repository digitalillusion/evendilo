package xyz.deverse.evendilo.model.woocommerce

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.JsonNode
import xyz.deverse.evendilo.model.Model


data class Category (
    override val id: Long?,
    val name: String
) : Model

data class Image (
    var src: String
)

enum class ProductType {
    simple, grouped, external, variable;

    companion object {
        fun valueOf(type: String): ProductType {
            return when (type) {
                simple.toString() -> simple
                grouped.toString() -> grouped
                external.toString() -> external
                variable.toString() -> variable
                else -> throw IllegalArgumentException(type)
            }
        }
    }
}

data class Attribute (
        override val id: Long?,
        var name: String = "",
        var options: MutableList<AttributeTerm> = mutableListOf()
): Model {
    val option: String?
        get() =  if (options.size > 0) { options[0].name } else { null }
}

data class AttributeTerm (
        override val id: Long?,
        var name: String
): Model {
    constructor() : this(null, "")

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(name: String) : this(null, name)
}

data class Product(
        override var id: Long?,
        var sku: String,
        var name: String,
        var type: ProductType,
        var regular_price: String,
        var sale_price: String,
        var description: String,
        var short_description: String,
        var categories: MutableList<Category>,
        var images: MutableList<Image>,
        var attributes: MutableList<Attribute>,
        var variations: MutableList<ProductVariation>
) : Model {
    constructor(): this(null, "", "", ProductType.simple, "", "", "", "",
            mutableListOf<Category>(), mutableListOf<Image>(), mutableListOf<Attribute>(),
            mutableListOf<ProductVariation>())
}

data class ProductVariation (
        override val id: Long?,
        var sku: String,
        var description: String,
        var regular_price: String,
        var sale_price: String,
        var attributes: MutableList<Attribute>
) : Model {
    constructor() : this(null, "", "", "", "", mutableListOf<Attribute>())

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(id: Long) : this(id, "", "", "", "", mutableListOf<Attribute>())
}