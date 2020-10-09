package xyz.deverse.evendilo.model.woocommerce

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonValue
import xyz.deverse.evendilo.functions.replaceList
import xyz.deverse.evendilo.model.Model
import xyz.deverse.evendilo.model.ProductType
import java.io.File
import java.util.*
import kotlin.reflect.full.isSubclassOf

class WooCommerce {
    companion object {
        val LOCALE: Locale = Locale.ITALY;
    }
}

data class Tag (
        override val id: Long?,
        val name: String
) : Model

data class Category (
    override val id: Long?,
    val name: String
) : Model

@JsonInclude(Include.NON_NULL)
data class Image (
    override val id: Long?,
    var src: String = ""
) : Model

sealed class Attribute (
        override val id: Long?,
        open var name: String = "",
        open var position: Int = 0,
        open var options: MutableList<AttributeTerm> = mutableListOf()
): Model {

    fun asSingle() : Single {
        return Single(this.id, this.name, this.options[0])
    }

    fun copy(id: Long? = null, name: String? = null, position: Int? = null, options: MutableList<AttributeTerm>? = null): Attribute {
        if (options != null && options.size == 1 || this.options.size == 1) {
            return Single(id ?: this.id, name ?: this.name, options?.get(0) ?: this.options[0])
        }
        return Multiple(id ?: this.id, name ?: this.name, position ?: this.position, options ?: this.options)
    }

    class Multiple(id: Long?, name: String, position: Int, options: MutableList<AttributeTerm>) : Attribute(id, name, position, options) {
        val variation: Boolean = true
        val visible: Boolean = true
    }

    class Single(id: Long?, name: String, option: AttributeTerm) : Attribute(id, name, 0, mutableListOf(option)) {
        val option: AttributeTerm?
            get() = if (super.options.size > 0) { super.options[0] } else { null }

        override var name: String = ""
            @JsonIgnore
            get() = super.name

        override var position: Int = 0
            @JsonIgnore
            get() = super.position

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
            val position= (map["position"] ?: 0) as Int
            val options= map["options"]  ?: mutableListOf<String>()
            val terms = (options as MutableList<String>).map { AttributeTerm.Name(it) as AttributeTerm }.toMutableList()
            return Multiple(id.toLong(), name, position, terms)
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
        var description: String,
        var short_description: String,
        var categories: MutableList<Category>,
        var tags: MutableList<Tag>,
        var images: MutableList<Image>,
        var attributes: MutableList<Attribute>,
        var variations: MutableList<ProductVariation>
) : Model {
    constructor(): this(null, "", "", ProductType.Simple, "", "", "",
            mutableListOf<Category>(), mutableListOf<Tag>(), mutableListOf<Image>(), mutableListOf<Attribute>(),
            mutableListOf<ProductVariation>())

    fun from(product: Product) {
        this.id = product.id
        replaceList(this.images) {
            val imageName = "(.+?)(-\\d*)*\$".toRegex().find(File(it.src).nameWithoutExtension)?.groupValues!![1]
            product.images.find { image -> image.src.contains(imageName) } ?: it
        }
        this.variations = product.variations
    }
}

data class ProductVariation (
        override var id: Long?,
        var sku: String,
        var description: String,
        var regular_price: String,
        var image: Image,
        var attributes: MutableList<Attribute>
) : Model {
    constructor() : this(null, "", "", "", Image(null), mutableListOf<Attribute>())

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(id: Long) : this(id, "", "", "", Image(null), mutableListOf<Attribute>())

    fun from(productVariation: ProductVariation) {
        this.id = productVariation.id
    }
}