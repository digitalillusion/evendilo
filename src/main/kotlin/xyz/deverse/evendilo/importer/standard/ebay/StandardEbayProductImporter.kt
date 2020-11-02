package xyz.deverse.evendilo.importer.standard.ebay

import org.springframework.stereotype.Service
import xyz.deverse.evendilo.api.ebay.EbayApi
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
import xyz.deverse.evendilo.functions.mergeDistinct
import xyz.deverse.evendilo.functions.replaceList
import xyz.deverse.evendilo.importer.standard.EvendiloCsvLine
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.ProductType
import xyz.deverse.evendilo.model.ebay.Product
import xyz.deverse.evendilo.model.woocommerce.Attribute
import xyz.deverse.importer.AbstractImporter
import xyz.deverse.importer.ImportMapper
import xyz.deverse.importer.csv.CsvColumn
import java.util.stream.Collectors

data class StandardEbayProductCsvLine(
        @CsvColumn(1) var sku: String = "",
        @CsvColumn(2) var name: String = "",
        @CsvColumn(3) var type: String = "",
        @CsvColumn(4) var taxed_price: String = "",
        @CsvColumn(5) var regular_price: String = "",
        @CsvColumn(6) var stock_quantity: String = "",
        @CsvColumn(7) var short_description: String = "",
        @CsvColumn(8) var description: String = "",
        @CsvColumn(9) var categoryNames: String = "",
        @CsvColumn(10) var tagNames: String = "",
        @CsvColumn(11) var imageUrls: String = ""
) : EvendiloCsvLine<Product>()

@Service
class StandardEbayProductImporter(var api: EbayApi, var appConfigProperties: AppConfigurationProperties) :
        AbstractImporter<Product, ImportMapper.MappedLine<Product>>(Family.Standard, Destination.Ebay) {

    override fun preProcess() {
        api.refreshCache()
    }

    override fun onParseLine(line: ImportMapper.MappedLine<Product>) {
        replaceList(line.nodes) { node ->
            replaceList(node.product.imageUrls) { image ->
                var imageUrl = image
                if (image.startsWith("/")) {
                    imageUrl = appConfigProperties.ebayConfig().importerConfig.imageUploadBaseUrl + image
                }
                imageUrl
            }
            node.offer.categoryId = if (node.offer.categoryId.isBlank()) { api.getCategorySuggestions(node.product) } else { node.offer.categoryId }

            var validAspects = node.product.aspects
                    .filter { entry -> entry.value.joinToString().isNotBlank() }
                    .toMutableMap()

            when (node.type) {
                ProductType.Simple -> {
                    node.sku = node.offer.sku
                    mergeExisting(node)
                    node
                }
                ProductType.Variable -> {
                    node.sku = node.offer.sku
                    mergeExisting(node)
                    node.product.aspects = prepareAttributes(validAspects, node)
                    node
                }
                ProductType.Variation -> {
                    node.sku = node.offer.sku + "_" + validAspects
                            .map { a -> a.value.stream().collect(Collectors.joining("|")) }
                            .joinToString("_")
                            .replace("\\s+".toRegex(), "-")
                    mergeExisting(node)
                    node.product.aspects = prepareAttributes(validAspects, node)
                    node
                }
            }

            node
        }
    }

    private fun mergeExisting(node: Product) {
        val existing = api.findProduct(node)
        existing?.let {
            node.product.imageUrls = it.product.imageUrls
            node.from(it)
        }
    }

    private fun prepareAttributes(validAttributes: MutableMap<String, MutableList<String>>, result: Product) : HashMap<String, MutableList<String>> {
        var prepared = LinkedHashMap<String, MutableList<String>>();
        validAttributes.keys.forEach { a ->
            val existingTerms = result.product.aspects.entries.find { it.key == a }?.value ?: mutableListOf()
            mergeDistinct(validAttributes[a]!!, existingTerms)
            prepared[a] = validAttributes[a]!!
        }
        return prepared
    }
}