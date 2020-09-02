package xyz.deverse.evendilo.importer.standard.ebay.mappers;

import org.mapstruct.*
import org.springframework.stereotype.Component
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
import xyz.deverse.evendilo.importer.standard.EbayEntityFactory
import xyz.deverse.evendilo.importer.standard.EvendiloCsvLine
import xyz.deverse.evendilo.importer.standard.ebay.StandardEbayProductCsvLine
import xyz.deverse.evendilo.model.ebay.Product
import xyz.deverse.importer.csv.CsvFileReader

@Component
class EbayProductMapperHelper {
    @Named("toImages")
    fun toImages(imageUrls: String): MutableList<String> {
        return if (imageUrls.isNotEmpty()) {
            imageUrls.split(",")
                    .filter { it.isNotBlank() }
                    .toMutableList()
        } else {
            mutableListOf()
        }
    }
}

@Mapper(componentModel = "spring",
        uses = [EbayProductMapperHelper::class, EbayEntityFactory::class, StandardEbayProductMapper.Finalizer::class],
        imports=[EvendiloCsvLine::class],
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
interface StandardEbayProductMapper : CsvFileReader.CsvImportMapper<Product, StandardEbayProductCsvLine> {

    @Component
    class Finalizer(var appConfigProperties: AppConfigurationProperties, var entityFactory: EbayEntityFactory) {
        @AfterMapping
        fun mapAttributes(csvLine: StandardEbayProductCsvLine, @MappingTarget product: Product) {
            val (attributes, csvLineAttrs) = entityFactory.getAttributesMapping(appConfigProperties, csvLine)

            attributes.forEachIndexed { index, attribute ->
                product.product.aspects[attribute] = mutableListOf(csvLineAttrs[index])
            }
        }
    }

    @Mappings(value = [
        Mapping(target = "id", ignore = true),
        Mapping(target = "availability.shipToLocationAvailability.quantity", expression = "java(1)"),
        Mapping(target = "condition", expression = "java(\"NEW\")"),
        Mapping(target = "product.title", source = "imageUrls"),
        Mapping(target = "product.description", expression = "java(org.apache.commons.text.StringEscapeUtils.escapeJava(standardEbayProductCsvLine.getDescription()))"),
        Mapping(target = "product.imageUrls", source = "imageUrls", qualifiedByName = ["toImages"])
    ])
    override fun toNode(standardEbayProductCsvLine: StandardEbayProductCsvLine): Product
}