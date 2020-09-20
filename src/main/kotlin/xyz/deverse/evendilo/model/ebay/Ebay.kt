package xyz.deverse.evendilo.model.ebay

import xyz.deverse.evendilo.api.ebay.EbayApi
import xyz.deverse.evendilo.model.Model
import xyz.deverse.evendilo.model.ProductType

data class Address (
    var postalCode: String = "0",
    var country: String = "IT"
)

data class Location (
    var address: Address = Address()
)

data class InventoryLocation (
    var location: Location = Location(),
    var locationTypes: List<String> = listOf ("WAREHOUSE"),
    var name: String = EbayApi.EVENDILO_INVENTORY_LOCATION
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
        var type: ProductType,
        var availability: Availability = Availability(),
        var condition: String = "",
        var product: ProductInfo = ProductInfo()
) : Model {
    constructor() : this(null, ProductType.Simple)
}