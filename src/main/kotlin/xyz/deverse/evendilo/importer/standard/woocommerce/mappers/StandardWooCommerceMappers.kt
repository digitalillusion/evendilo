package xyz.deverse.evendilo.importer.standard.woocommerce.mappers

import org.mapstruct.*
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Component
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
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
    class Finalizer(var appConfigProperties: AppConfigurationProperties) {
        @AfterMapping
        fun mapAttributes(csvLine: StandardWooCommerceProductCsvLine, @MappingTarget product: Product) {
            val attributes = mutableListOf<String>()
            var token = SecurityContextHolder.getContext().authentication as OAuth2AuthenticationToken
            for (config in appConfigProperties.woocommerce) {
                val importerConfig = config.importerConfig;
                if (token.authorizedClientRegistrationId == config.identifier) {
                    importerConfig.attributes.split(",").toList().map { it.trim() }.forEach { attributes.add(it) }
                    break
                }
            }
            val csvLineAttrs = arrayOf(csvLine.attr0, csvLine.attr1, csvLine.attr2, csvLine.attr3,
                    csvLine.attr4, csvLine.attr5, csvLine.attr6, csvLine.attr7, csvLine.attr8, csvLine.attr9)

            product.attributes = mutableListOf()
            attributes.forEachIndexed { index, attribute ->
                product.attributes.add(Attribute(null, attribute, mutableListOf(AttributeTerm.Name(csvLineAttrs[index])))
            )}
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