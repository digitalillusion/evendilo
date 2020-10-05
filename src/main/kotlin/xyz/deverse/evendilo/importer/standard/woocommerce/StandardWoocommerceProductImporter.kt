package xyz.deverse.evendilo.importer.standard.woocommerce

import org.springframework.stereotype.Service
import xyz.deverse.evendilo.api.woocommerce.WoocommerceApi
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
import xyz.deverse.evendilo.functions.mergeDistinct
import xyz.deverse.evendilo.functions.replaceList
import xyz.deverse.evendilo.functions.replaceListIndexed
import xyz.deverse.evendilo.importer.ErrorCode
import xyz.deverse.evendilo.importer.ImportLineException
import xyz.deverse.evendilo.importer.standard.EvendiloCsvLine
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.ProductType
import xyz.deverse.evendilo.model.woocommerce.Attribute
import xyz.deverse.evendilo.model.woocommerce.Product
import xyz.deverse.evendilo.model.woocommerce.ProductVariation
import xyz.deverse.importer.AbstractImporter
import xyz.deverse.importer.ImportMapper
import xyz.deverse.importer.csv.CsvColumn
import java.io.File


data class StandardWoocommerceProductCsvLine(
        @CsvColumn(0) var sku: String = "",
        @CsvColumn(1) var name: String = "",
        @CsvColumn(2) var type: String = "",
        @CsvColumn(3) var taxed_price: String = "",
        @CsvColumn(4) var regular_price: String = "",
        @CsvColumn(5) var short_description: String = "",
        @CsvColumn(6) var description: String = "",
        @CsvColumn(7) var categoryNames: String = "",
        @CsvColumn(8) var tagNames: String = "",
        @CsvColumn(9) var imageUrls: String = ""
) : EvendiloCsvLine<Product>()

@Service
class StandardWoocommerceProductImporter(var api: WoocommerceApi, var appConfigProperties: AppConfigurationProperties) :
        AbstractImporter<Product, ImportMapper.MappedLine<Product>>(Family.Standard, Destination.Woocommerce) {

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
            replaceList(node.images) { image ->
                if (image.src.startsWith("/")) {
                    image.src = appConfigProperties.woocommerceConfig().importerConfig.imageUploadBaseUrl + image.src
                }
                image
            }
            replaceListIndexed(node.attributes) { index, nodeAttribute ->
                val existing = api.findAttribute(nodeAttribute)
                if (existing != null) {
                    val existingOptions = api.findAttributeTerms(existing)
                    replaceList(nodeAttribute.options) { nodeAttributeOption ->
                        val existingOption = existingOptions.find { it.name == nodeAttributeOption.name }
                        existingOption ?: nodeAttributeOption
                    }
                    Attribute.Multiple(existing.id, existing.name, index, nodeAttribute.options)
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

            val existing = api.findProduct(node)

            when (node.type) {
                ProductType.Simple-> {
                    existing?.let { node.from(it) }
                    node
                }
                ProductType.Variable -> {
                    existing?.let {
                        node.images = it.images
                        node.from(it)
                    }
                    node.attributes = validAttributes.mapIndexed { index, a ->
                        val existingTerms = node.attributes.find { it.name == a.name }?.options ?: mutableListOf()
                        mergeDistinct(a.options, existingTerms)
                        a.position = index
                        a
                    }.toMutableList()
                    node
                }
                ProductType.Variation -> {
                    val result = existing ?: node
                    result.attributes = validAttributes.mapIndexed { index, a ->
                        val existingTerms = result.attributes.find { it.name == a.name }?.options ?: mutableListOf()
                        mergeDistinct(a.options, existingTerms)
                        a.position = index
                        a
                    }.toMutableList()

                    val imageName = "(.+?)(-\\d*)*\$".toRegex().find(File(node.images[0].src).nameWithoutExtension)?.groupValues!![1]
                    val image = result.images.find { it.src.contains(imageName) } ?: node.images[0]
                    val sku = node.sku + "_" + variationAttributes.map { a: Attribute -> a.asSingle().option?.asName()?.name }.joinToString("_").replace("\\s+".toRegex(), "-")
                    val description = if (node != result) { "" } else { node.description }
                    val variation = ProductVariation(
                            null,
                            sku,
                            description,
                            node.regular_price,
                            image,
                            variationAttributes
                    )
                    val existingVariation = api.findProductVariation(result, variation)
                    existingVariation?.let {
                        variation.from(it)
                        result.variations.removeIf { v -> it.id == v.id }
                    }
                    result.variations.add(variation)
                    result
                }
                else -> {
                    throw ImportLineException(ErrorCode.IMPORT_LINE_ERROR_PRODUCT_TYPE)
                }
            }
        }

    }

}