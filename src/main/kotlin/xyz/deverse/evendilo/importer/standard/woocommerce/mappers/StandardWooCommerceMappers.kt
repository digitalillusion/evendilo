package xyz.deverse.evendilo.importer.standard.woocommerce.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.Qualifier
import org.springframework.stereotype.Component
import xyz.deverse.evendilo.importer.standard.woocommerce.StandardWooCommerceProductCsvLine
import xyz.deverse.evendilo.model.woocommerce.Category
import xyz.deverse.evendilo.model.woocommerce.Image
import xyz.deverse.evendilo.model.woocommerce.Product
import xyz.deverse.importer.csv.CsvFileReader

@Qualifier
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ImageMap

@Qualifier
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class CategoryMap

@Component
class ImageMapper {
    @ImageMap
    fun toImages(imageUrls: String): MutableList<Image> {
        return if (imageUrls.isNotEmpty()) {
            imageUrls.split(",").map { Image(it.trim()) }.toMutableList()
        } else {
            mutableListOf()
        }
    }
}

@Component
class CategoryMapper {
    @CategoryMap
    fun toCategory(categoryNames: String): MutableList<Category> {
        return if (categoryNames.isNotEmpty()) {
            categoryNames.split(",").map { Category(null, it.trim()) }.toMutableList()
        } else {
            mutableListOf()
        }
    }
}

@Mapper(componentModel = "spring", uses = [ImageMapper::class, CategoryMapper::class], imports=[Image::class,  Category::class])
interface StandardWooCommerceProductMapper : CsvFileReader.CsvImportMapper<Product, StandardWooCommerceProductCsvLine> {

    @Mappings(value = [
        Mapping(target = "id", ignore = true),
        Mapping(source = "imageUrls", target = "images", qualifiedBy = [ImageMap::class]),
        Mapping(source = "categoryNames", target = "categories", qualifiedBy = [CategoryMap::class])
    ])
    override fun toNode(csvLine: StandardWooCommerceProductCsvLine): Product
}