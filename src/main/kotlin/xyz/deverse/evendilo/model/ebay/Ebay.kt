package xyz.deverse.evendilo.model.ebay

import xyz.deverse.evendilo.model.Model
import xyz.deverse.evendilo.model.woocommerce.Attribute
import xyz.deverse.evendilo.model.woocommerce.Image

data class ShipToLocationAvailability (
    var quantity: Long = 0
)

data class Availability (
    var shipToLocationAvailability : ShipToLocationAvailability = ShipToLocationAvailability()
)

data class ProductInfo (
    var title: String = "",
    var description: String = "",
    var aspects: MutableMap<String, MutableList<String>> = mutableMapOf(),
//    var brand: String,
//    var mpn: String,
    var imageUrls: MutableList<String> = mutableListOf()
)

data class Product (
    override var id: Long?,
    var availability: Availability = Availability(),
    var condition: String = "",
    var product: ProductInfo = ProductInfo()
) : Model {
    constructor() : this(null)
}