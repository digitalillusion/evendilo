package xyz.deverse.evendilo.model.ebay

import xyz.deverse.evendilo.model.Model

data class Address (
    var country: String = "it"
)

data class InventoryLocation (
    var address: Address = Address()
)

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