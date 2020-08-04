package xyz.deverse.evendilo.model.woocommerce

import xyz.deverse.evendilo.model.Model

data class Category (
    override val id: Long?,
    val name: String
) : Model

data class Image (
    var src: String
)

data class Product(
    override val id: Long?,
    var name: String,
    var type: String,
    var regular_price: String,
    var description: String,
    var short_description: String,
    var categories: MutableList<Category>,
    var images: MutableList<Image>
) : Model