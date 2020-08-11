package xyz.deverse.evendilo.importer.standard.woocommerce.mappers

import org.mapstruct.*
import org.springframework.stereotype.Component
import xyz.deverse.evendilo.importer.standard.WooCommerceEntityFactory
import xyz.deverse.evendilo.importer.standard.woocommerce.StandardWooCommerceProductCsvLine
import xyz.deverse.evendilo.model.woocommerce.*
import xyz.deverse.importer.csv.CsvFileReader

@Component
class ProductMapperHelper {
    @Named("toImage")
    fun toImages(imageUrls: String): MutableList<Image> {
        return if (imageUrls.isNotEmpty()) {
            imageUrls.split(",").map { Image(it.trim()) }.toMutableList()
        } else {
            mutableListOf()
        }
    }

    @Named("toCategory")
    fun toCategory(categoryNames: String): MutableList<Category> {
        return if (categoryNames.isNotEmpty()) {
            categoryNames.split(",").map { Category(null, it.trim()) }.toMutableList()
        } else {
            mutableListOf()
        }
    }

}

@Mapper(componentModel = "spring",
        uses = [ProductMapperHelper::class, WooCommerceEntityFactory::class, StandardWooCommerceProductMapper.Finalizer::class],
        imports=[ProductType::class, Image::class, Category::class, Attribute::class],
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
interface StandardWooCommerceProductMapper : CsvFileReader.CsvImportMapper<Product, StandardWooCommerceProductCsvLine> {

    @Component
    class Finalizer {
        @AfterMapping
        fun mapAttributes(csvLine: StandardWooCommerceProductCsvLine, @MappingTarget product: Product) {
            // TODO: extract attr name
            product.attributes = mutableListOf(
                    Attribute(null, "DISEGNO", mutableListOf(AttributeTerm(null, csvLine.design))),
                    Attribute(null, "COLORE", mutableListOf(AttributeTerm(null, csvLine.color))),
                    Attribute(null, "MISURA", mutableListOf(AttributeTerm(null, csvLine.size)))
            )
        }
    }

    @Mappings(value = [
        Mapping(target = "id", ignore = true),
        Mapping(target = "type", expression = "java(ProductType.variable)"),
        Mapping(source = "imageUrls", target = "images", qualifiedByName = ["toImage"]),
        Mapping(source = "categoryNames", target = "categories", qualifiedByName = ["toCategory"])
    ])
    override fun toNode(csvLine: StandardWooCommerceProductCsvLine): Product
}