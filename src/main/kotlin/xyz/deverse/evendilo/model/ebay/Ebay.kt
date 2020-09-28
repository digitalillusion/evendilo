package xyz.deverse.evendilo.model.ebay

import xyz.deverse.evendilo.api.ebay.EbayApi
import xyz.deverse.evendilo.model.Model
import xyz.deverse.evendilo.model.ProductType
import java.util.*

class EbayConstants {
    companion object {
        val LOCALE: Locale = Locale.ITALY;
        const val COUNTRY: String = "IT";
        const val CONTENT_LANGUAGE: String = "it-IT";
        const val MARKETPLACE_ID: String = "EBAY_IT"
    }
}

data class Response (
    val warnings: MutableList<Error> = mutableListOf(),
    val errors: MutableList<Error> = mutableListOf()
)

data class Address (
    var postalCode: String = "0",
    var country: String = EbayConstants.COUNTRY
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
    var imageUrls: MutableList<String> = mutableListOf()
)

data class Product (
        override var id: Long?,
        var type: ProductType,
        var sku: String = "",
        var availability: Availability = Availability(),
        var condition: String = "",
        var product: ProductInfo = ProductInfo(),
        var offer: Offer = Offer()
) : Model {
    constructor() : this(null, ProductType.Simple)
}

data class Amount (
    var currency: String = "EUR",
    var value: Float = 0f
)


data class PricingSummary (
    var price: Amount = Amount()
)

data class Offer (
        override var id: Long?,
        var format: String = "FIXED_PRICE",
        var marketplaceId: String = EbayConstants.MARKETPLACE_ID,
        var sku: String = "",
        var categoryId: String = "",
        var pricingSummary: PricingSummary = PricingSummary(),
        var merchantLocationKey: String = EbayApi.EVENDILO_INVENTORY_LOCATION,
        var storeCategoryNames: MutableList<String> = mutableListOf()
) : Model {
    constructor() : this(null)

    var offerId: Long?
        get() = id
        set(value) { id = value }
}