package xyz.deverse.evendilo.importer.standard.woocommerce


import org.springframework.stereotype.Service
import xyz.deverse.evendilo.api.woocommerce.StandardWooCommerceApi
import xyz.deverse.evendilo.importer.ErrorCode
import xyz.deverse.evendilo.importer.ImportLineException
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.woocommerce.Product
import xyz.deverse.importer.AbstractImporter
import xyz.deverse.importer.ImportMapper
import xyz.deverse.importer.csv.CsvColumn
import xyz.deverse.importer.csv.CsvFileReader


data class StandardWooCommerceProductCsvLine(
        @CsvColumn(0) var name: String = "",
        @CsvColumn(1) var type: String = "",
        @CsvColumn(2) var regular_price: String = "",
        @CsvColumn(3) var description: String = "",
        @CsvColumn(4) var short_description: String = "",
        @CsvColumn(5) var categoryNames: String = "",
        @CsvColumn(6) var imageUrls: String = ""
) : CsvFileReader.CsvLine<Product>()

@Service
class StandardWooCommerceProductImporter(var api: StandardWooCommerceApi) :
        AbstractImporter<Product, ImportMapper.MappedLine<Product>>(Family.Standard, Destination.WooCommerce) {

    override fun reinitialize() {
        api.refreshCache()
    }

    override fun onParseLine(line: ImportMapper.MappedLine<Product>) {
        line.nodes.forEach { node ->
            val categoryNames = node.categories.map { it.name }
            categoryNames.forEach { categoryName ->
                val category = api.findCategory(categoryName) ?: throw ImportLineException(ErrorCode.IMPORT_LINE_ERROR_WRONG_CATEGORY)
                node.categories.replaceAll { c ->
                    return@replaceAll if (c.name == categoryName) { category } else { c }
                }
            }

        }

    }

}