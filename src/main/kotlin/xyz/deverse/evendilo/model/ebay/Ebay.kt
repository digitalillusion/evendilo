package xyz.deverse.evendilo.model.ebay

import com.fasterxml.jackson.annotation.JsonIgnore
import xyz.deverse.evendilo.api.ebay.EbayApi
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.Model
import xyz.deverse.evendilo.model.ProductType

class EbayConstants {
    companion object {
        const val COUNTRY: String = "IT";
        const val CONTENT_LANGUAGE: String = "it-IT";
        const val MARKETPLACE_ID: String = "EBAY_IT"
    }
}

interface EbayModel : Model {
    @get:JsonIgnore
    override val family
        get() = Family.Standard

    @get:JsonIgnore
    override val destination
        get() = Destination.Ebay
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

data class Variation (
    val name: String,
    val values: MutableList<String>
)

data class VariationContainer(
    var specifications: MutableSet<Variation> = mutableSetOf()
) {
    constructor(aspects: MutableMap<String, MutableList<String>>) : this () {
        aspects.forEach { aspect ->  specifications.add(Variation(aspect.key, aspect.value)) }
    }
}

data class Group (
    override val id: Long?,
    var title: String = "",
    var sku: String = "",
    var description: String = "",
    var aspects: MutableMap<String, MutableList<String>> = mutableMapOf(),
    var imageUrls: MutableList<String> = mutableListOf(),
    var variantSKUs: MutableList<String> = mutableListOf(),
    var variesBy: VariationContainer = VariationContainer()
) : EbayModel {
    constructor(product: Product, variantSKUs: MutableList<String>) :
            this(null, product.product.title, product.sku, product.product.description, product.product.aspects, product.product.imageUrls, variantSKUs) {
        variesBy =  VariationContainer(aspects)
    }

    val inventoryItemGroupKey: String?
        get() = this.sku
}

data class Product (
        var type: ProductType = ProductType.Simple,
        var sku: String = "",
        var availability: Availability = Availability(),
        var condition: String = "",
        var product: ProductInfo = ProductInfo(),
        var offer: Offer = Offer()
) : EbayModel {
    fun from(product: Product) {
        this.type = product.type
        this.product.imageUrls = product.product.imageUrls
        this.offer.from(product.offer)
    }

    private var _hashcodeId: Long? = sku.hashCode().toLong()
    override var id: Long?
        get() = _hashcodeId
        set(value) { _hashcodeId = value }
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
) : EbayModel {
    fun from(offer: Offer) {
        this.id = offer.id
        this.sku = offer.sku;
    }

    constructor() : this(null)

    var offerId: Long?
        get() = id
        set(value) { id = value }
}