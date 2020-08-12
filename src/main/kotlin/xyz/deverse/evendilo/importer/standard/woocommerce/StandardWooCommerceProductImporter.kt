package xyz.deverse.evendilo.importer.standard.woocommerce


import org.springframework.stereotype.Service
import xyz.deverse.evendilo.api.woocommerce.WooCommerceApi
import xyz.deverse.evendilo.functions.replaceList
import xyz.deverse.evendilo.importer.ErrorCode
import xyz.deverse.evendilo.importer.ImportLineException
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.woocommerce.Attribute
import xyz.deverse.evendilo.model.woocommerce.Product
import xyz.deverse.evendilo.model.woocommerce.ProductVariation
import xyz.deverse.importer.AbstractImporter
import xyz.deverse.importer.ImportMapper
import xyz.deverse.importer.csv.CsvColumn
import xyz.deverse.importer.csv.CsvFileReader


data class StandardWooCommerceProductCsvLine(
        var type: String = "",
        @CsvColumn(0) var sku: String = "",
        @CsvColumn(1) var name: String = "",
        @CsvColumn(2) var sale_price: String = "",
        @CsvColumn(3) var regular_price: String = "",
        @CsvColumn(4) var short_description: String = "",
        @CsvColumn(5) var description: String = "",
        @CsvColumn(6) var categoryNames: String = "",
        @CsvColumn(7) var imageUrls: String = "",

        @CsvColumn(8) var attr0: String = "",
        @CsvColumn(9) var attr1: String = "",
        @CsvColumn(10) var attr2: String = "",
        @CsvColumn(11) var attr3: String = "",
        @CsvColumn(12) var attr4: String = "",
        @CsvColumn(13) var attr5: String = "",
        @CsvColumn(14) var attr6: String = "",
        @CsvColumn(15) var attr7: String = "",
        @CsvColumn(16) var attr8: String = "",
        @CsvColumn(17) var attr9: String = ""
) : CsvFileReader.CsvLine<Product>()

@Service
class StandardWooCommerceProductImporter(var api: WooCommerceApi) :
        AbstractImporter<Product, ImportMapper.MappedLine<Product>>(Family.Standard, Destination.WooCommerce) {

    override fun reinitialize() {
        api.refreshCache()
    }

    override fun onParseLine(line: ImportMapper.MappedLine<Product>) {
        replaceList (line.nodes) { node ->
            replaceList(node.categories) { nodeCategory ->
                api.findCategory(nodeCategory)
                        ?: throw ImportLineException(ErrorCode.IMPORT_LINE_ERROR_PRODUCT_CATEGORY)
            }
            replaceList(node.attributes) { nodeAttribute ->
                var existing = api.findAttribute(nodeAttribute)
                if (existing != null) {
                    val existingOptions = api.findAttributeTerms(existing)
                    replaceList(nodeAttribute.options) { nodeAttributeOption ->
                        val existingOption = existingOptions.find { it.name == nodeAttributeOption.name }
                        existingOption ?: nodeAttributeOption
                    }
                    Attribute(existing.id, existing.name, nodeAttribute.options)
                } else {
                    nodeAttribute
                }
            }

            var validNodeAttributes = node.attributes.filter { attribute: Attribute ->
                attribute.name.isNotBlank() && attribute.option?.isNotBlank() ?: false
            }.toMutableList()

            var result = api.findProduct(node) ?: node
            result.variations.add(ProductVariation(
                null,
                node.sku + "_" + validNodeAttributes.map { a: Attribute -> a.option }.joinToString("_"),
                result.description,
                result.regular_price,
                result.sale_price,
                validNodeAttributes
            ))
            result
        }

    }

}