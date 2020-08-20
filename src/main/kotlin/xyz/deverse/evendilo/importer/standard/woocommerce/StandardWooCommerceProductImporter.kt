package xyz.deverse.evendilo.importer.standard.woocommerce

import org.springframework.stereotype.Service
import xyz.deverse.evendilo.api.woocommerce.WooCommerceApi
import xyz.deverse.evendilo.functions.mergeDistinct
import xyz.deverse.evendilo.functions.replaceList
import xyz.deverse.evendilo.importer.ErrorCode
import xyz.deverse.evendilo.importer.ImportLineException
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.woocommerce.Attribute
import xyz.deverse.evendilo.model.woocommerce.Product
import xyz.deverse.evendilo.model.woocommerce.ProductType
import xyz.deverse.evendilo.model.woocommerce.ProductVariation
import xyz.deverse.importer.AbstractImporter
import xyz.deverse.importer.ImportMapper
import xyz.deverse.importer.csv.CsvColumn
import xyz.deverse.importer.csv.CsvFileReader
import java.io.File


data class StandardWooCommerceProductCsvLine(
        @CsvColumn(0) var sku: String = "",
        @CsvColumn(1) var name: String = "",
        @CsvColumn(2) var type: String = "",
        @CsvColumn(3) var sale_price: String = "",
        @CsvColumn(4) var regular_price: String = "",
        @CsvColumn(5) var short_description: String = "",
        @CsvColumn(6) var description: String = "",
        @CsvColumn(7) var categoryNames: String = "",
        @CsvColumn(8) var tagNames: String = "",
        @CsvColumn(9) var imageUrls: String = "",

        @CsvColumn(10) var attr0: String = "",
        @CsvColumn(11) var attr1: String = "",
        @CsvColumn(12) var attr2: String = "",
        @CsvColumn(13) var attr3: String = "",
        @CsvColumn(14) var attr4: String = "",
        @CsvColumn(15) var attr5: String = "",
        @CsvColumn(16) var attr6: String = "",
        @CsvColumn(17) var attr7: String = "",
        @CsvColumn(18) var attr8: String = "",
        @CsvColumn(19) var attr9: String = ""
) : CsvFileReader.CsvLine<Product>()

@Service
class StandardWooCommerceProductImporter(var api: WooCommerceApi) :
        AbstractImporter<Product, ImportMapper.MappedLine<Product>>(Family.Standard, Destination.WooCommerce) {

    override fun preProcess() {
        api.refreshCache()
    }

    override fun onParseLine(line: ImportMapper.MappedLine<Product>) {
        replaceList (line.nodes) { node ->
            replaceList(node.categories) { nodeCategory ->
                api.findCategory(nodeCategory)
                        ?: throw ImportLineException(ErrorCode.IMPORT_LINE_ERROR_PRODUCT_CATEGORY)
            }
            replaceList(node.tags) { nodeTag ->
                api.findTag(nodeTag)
                        ?: throw ImportLineException(ErrorCode.IMPORT_LINE_ERROR_PRODUCT_TAG)
            }
            replaceList(node.attributes) { nodeAttribute ->
                val existing = api.findAttribute(nodeAttribute)
                if (existing != null) {
                    val existingOptions = api.findAttributeTerms(existing)
                    replaceList(nodeAttribute.options) { nodeAttributeOption ->
                        val existingOption = existingOptions.find { it.name == nodeAttributeOption.name }
                        existingOption ?: nodeAttributeOption
                    }
                    Attribute.Multiple(existing.id, existing.name, nodeAttribute.options)
                } else {
                    nodeAttribute
                }
            }

            val validAttributes = node.attributes.filter { attribute: Attribute ->
                attribute.name.isNotBlank() && attribute.asSingle().option?.asName()?.name?.isNotBlank() ?: false
            }.toMutableList()
            val variationAttributes = mutableListOf<Attribute>()
            validAttributes.forEach { attribute ->
                val optionsCopy = attribute.options.map { o -> o.copy() }.toMutableList()
                variationAttributes.add(attribute.copy(options = optionsCopy))
            }

            when (node.type) {
                ProductType.Simple -> {
                    node
                }
                ProductType.Variable -> {
                    val result = api.findProduct(node) ?: node
                    result.attributes = validAttributes.map { a ->
                        val existingTerms = result.attributes.find { it.name == a.name }?.options ?: mutableListOf()
                        mergeDistinct(a.options, existingTerms)
                        a
                    }.toMutableList()

                    val imageName = File(node.images[0].src).nameWithoutExtension
                    val image = result.images.find { it.src.contains(imageName) } ?: node.images[0]
                    val sku = node.sku + "_" + variationAttributes.map { a: Attribute -> a.asSingle().option?.asName()?.name }.joinToString("_").replace("\\s+".toRegex(), "-")
                    val description = if (node != result) {
                        ""
                    } else {
                        node.description
                    }
                    result.variations.add(ProductVariation(
                            null,
                            sku,
                            description,
                            node.regular_price,
                            node.sale_price,
                            image,
                            variationAttributes
                    ))
                    result
                }
                else -> {
                    throw ImportLineException(ErrorCode.IMPORT_LINE_ERROR_PRODUCT_TYPE)
                }
            }
        }

    }

}