package xyz.deverse.evendilo.importer.standard.ebay

import org.springframework.stereotype.Service
import xyz.deverse.evendilo.api.ebay.EbayApi
import xyz.deverse.evendilo.functions.replaceList
import xyz.deverse.evendilo.importer.standard.EvendiloCsvLine
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.ProductType
import xyz.deverse.evendilo.model.ebay.Product
import xyz.deverse.importer.AbstractImporter
import xyz.deverse.importer.ImportMapper
import xyz.deverse.importer.csv.CsvColumn

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
class StandardEbayProductImporter(var api: EbayApi) :
        AbstractImporter<Product, ImportMapper.MappedLine<Product>>(Family.Standard, Destination.Ebay) {

    override fun preProcess() {
        api.refreshCache()
    }

    override fun onParseLine(line: ImportMapper.MappedLine<Product>) {
        replaceList(line.nodes) { node ->
            node.product.aspects = node.product.aspects
                    .filter { entry -> entry.value.joinToString().isNotBlank() }
                    .toMutableMap()

            when (node.type) {
                ProductType.Simple -> {
                    node.offer.categoryId = api.getCategorySuggestions()
                }
                ProductType.Grouped,
                ProductType.Variation,
                ProductType.External,
                ProductType.Variable -> {

                }
            }

            node
        }
    }
}