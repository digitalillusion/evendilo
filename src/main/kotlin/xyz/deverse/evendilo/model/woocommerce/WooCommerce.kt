package xyz.deverse.evendilo.model.woocommerce

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonValue
import xyz.deverse.evendilo.model.Model
import kotlin.reflect.full.isSubclassOf


data class Category (
    override val id: Long?,
    val name: String
) : Model

@JsonInclude(Include.NON_NULL)
data class Image (
    override val id: Long?,
    var src: String = ""
) : Model

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

sealed class Attribute (
        override val id: Long?,
        open var name: String = "",
        open var options: MutableList<AttributeTerm> = mutableListOf()
): Model {

    fun asSingle() : Single {
        return Single(this.id, this.name, this.options[0])
    }

    fun copy(id: Long? = null, name: String? = null, options: MutableList<AttributeTerm>? = null): Attribute {
        if (options != null && options.size == 1 || this.options.size == 1) {
            return Single(id ?: this.id, name ?: this.name, options?.get(0) ?: this.options[0])
        }
        return Multiple(id ?: this.id, name ?: this.name, options ?: this.options)
    }

    class Multiple(id: Long?, name: String, options: MutableList<AttributeTerm>) : Attribute(id, name, options) {
        val variation: Boolean = true
        val visible: Boolean = true
    }

    class Single(id: Long?, name: String, option: AttributeTerm) : Attribute(id, name, mutableListOf(option)) {
        val option: AttributeTerm?
            get() = if (super.options.size > 0) { super.options[0] } else { null }

        override var name: String = ""
            @JsonIgnore
            get() = super.name

        override var options: MutableList<AttributeTerm> = mutableListOf(option)
            @JsonIgnore
            get() = super.options
    }

    companion object {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        @JvmStatic
        private fun deserialize(obj: Any): Attribute {
            val map = obj as Map<*, *>
            val id= map["id"] as Int
            val name= map["name"] as String
            val options= map["options"]  ?: mutableListOf<String>()
            val terms = (options as MutableList<String>).map { AttributeTerm.Name(it) as AttributeTerm }.toMutableList()
            return Multiple(id.toLong(), name, terms)
        }
    }

}

sealed class AttributeTerm (
        override val id: Long?,
        var name: String
): Model {
    fun asName() : Name {
        return Name(name)
    }

    fun copy(id: Long? = null, name: String? = null): AttributeTerm {
        if (id == null && this.id == null) {
            return Name(name ?: this.name)
        }
        return Term(id ?: this.id!!, name ?: this.name)
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
        var image: Image,
        var attributes: MutableList<Attribute>
) : Model {
    constructor() : this(null, "", "", "", "", Image(null), mutableListOf<Attribute>())

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(id: Long) : this(id, "", "", "", "", Image(null), mutableListOf<Attribute>())
}