package xyz.deverse.evendilo.model.woocommerce

import com.fasterxml.jackson.annotation.*
import xyz.deverse.evendilo.model.Model
import kotlin.reflect.full.isSubclassOf


data class Category (
    override val id: Long?,
    val name: String
) : Model

data class Image (
    var src: String = ""
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
        @JsonIgnore
        get() =  if (options.size > 0) { options[0].name } else { null }
}

sealed class AttributeTerm (
        override val id: Long?,
        var name: String
): Model {
    constructor() : this(null, "")

    fun asName() : Name {
        return Name(name)
    }

    fun copy(id: Long? = null, name: String? = null): AttributeTerm {
        if (id == null && this.id == null) {
            return AttributeTerm.Name(name ?: this.name)
        }
        return AttributeTerm.Term(id ?: this.id!!, name ?: this.name)
    }

    class Name(name: String) : AttributeTerm(null, name) {
        @JsonValue
        override fun toString(): String {
            return name
        }
    }

    class Term(id: Long, name: String) : AttributeTerm(id, name)

    class NewTerm(id: Long?, name: String) : AttributeTerm(id, name)

    companion object {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        @JvmStatic
        private fun deserialize(obj: Any): AttributeTerm {
            if (obj::class.isSubclassOf(Map::class)) {
                val map = obj as Map<*, *>
                val id= map["id"] as Int?
                val name= map["name"] as String
                if (id == null) {
                    return Name(name)
                }
                return Term(id.toLong(), name)
            } else {
                return Name(obj.toString())
            }

        }
    }
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
        var images: Image,
        var attributes: MutableList<Attribute>
) : Model {
    constructor() : this(null, "", "", "", "", Image(), mutableListOf<Attribute>())

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(id: Long) : this(id, "", "", "", "", Image(), mutableListOf<Attribute>())
}