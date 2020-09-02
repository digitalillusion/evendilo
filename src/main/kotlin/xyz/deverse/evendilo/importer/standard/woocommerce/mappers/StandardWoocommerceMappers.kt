package xyz.deverse.evendilo.importer.standard.woocommerce.mappers

import org.mapstruct.*
import org.springframework.stereotype.Component
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
import xyz.deverse.evendilo.importer.standard.EvendiloCsvLine
import xyz.deverse.evendilo.importer.standard.WoocommerceEntityFactory
import xyz.deverse.evendilo.importer.standard.woocommerce.StandardWoocommerceProductCsvLine
import xyz.deverse.evendilo.model.woocommerce.*
import xyz.deverse.importer.csv.CsvFileReader

@Component
class WoocommerceProductMapperHelper {
    @Named("toImages")
    fun toImages(imageUrls: String): MutableList<Image> {
        return if (imageUrls.isNotEmpty()) {
            imageUrls.split(",")
                    .filter { it.isNotBlank() }
                    .map { Image(null, it.trim()) }.toMutableList()
        } else {
            mutableListOf()
        }
    }

    @Named("toCategories")
    fun toCategories(categoryNames: String): MutableList<Category> {
        return if (categoryNames.isNotEmpty()) {
            categoryNames.split(",")
                    .filter { it.isNotBlank() }
                    .map { Category(null, it.trim()) }.toMutableList()
        } else {
            mutableListOf()
        }
    }

    @Named("toTags")
    fun toTags(tagNames: String): MutableList<Tag> {
        return if (tagNames.isNotEmpty()) {
            tagNames.split(",")
                    .filter { it.isNotBlank() }
                    .map { Tag(null, it.trim()) }.toMutableList()
        } else {
            mutableListOf()
        }
    }
}

@Mapper(componentModel = "spring",
        uses = [WoocommerceProductMapperHelper::class, WoocommerceEntityFactory::class, StandardWoocommerceProductMapper.Finalizer::class],
        imports=[EvendiloCsvLine::class, ProductType::class, Image::class, Category::class, Attribute::class],
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
interface StandardWoocommerceProductMapper : CsvFileReader.CsvImportMapper<Product, StandardWoocommerceProductCsvLine> {

    @Component
    class Finalizer(var appConfigProperties: AppConfigurationProperties, var entityFactory: WoocommerceEntityFactory) {
        @AfterMapping
        fun mapAttributes(csvLine: StandardWoocommerceProductCsvLine, @MappingTarget product: Product) {
            val (attributes, csvLineAttrs) = entityFactory.getAttributesMapping(appConfigProperties, csvLine)

            product.attributes = mutableListOf()
            attributes.forEachIndexed { index, attribute ->
                product.attributes.add(Attribute.Multiple(null, attribute, index, mutableListOf(AttributeTerm.Name(csvLineAttrs[index])))
                )
            }
        }
    }

    @Mappings(value = [
        Mapping(target = "id", ignore = true),
        Mapping(target = "type", expression = "java(ProductType.fromString(standardWoocommerceProductCsvLine.getType()))"),
        Mapping(target = "description", expression = "java(org.apache.commons.text.StringEscapeUtils.escapeJava(standardWoocommerceProductCsvLine.getDescription()))"),
        Mapping(target = "short_description", expression = "java(org.apache.commons.text.StringEscapeUtils.escapeJava(standardWoocommerceProductCsvLine.getShort_description()))"),
        Mapping(source = "imageUrls", target = "images", qualifiedByName = ["toImages"]),
        Mapping(source = "categoryNames", target = "categories", qualifiedByName = ["toCategories"]),
        Mapping(source = "tagNames", target = "tags", qualifiedByName = ["toTags"])
    ])
    override fun toNode(standardWoocommerceProductCsvLine: StandardWoocommerceProductCsvLine): Product
}