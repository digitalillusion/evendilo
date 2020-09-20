package xyz.deverse.evendilo.importer.standard.amazon.mappers;

import org.mapstruct.*
import org.springframework.stereotype.Component
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
import xyz.deverse.evendilo.importer.standard.AmazonEntityFactory
import xyz.deverse.evendilo.importer.standard.EvendiloCsvLine
import xyz.deverse.evendilo.importer.standard.amazon.StandardAmazonProductCsvLine
import xyz.deverse.evendilo.importer.standard.ebay.StandardEbayProductCsvLine
import xyz.deverse.evendilo.model.amazon.Product
import xyz.deverse.importer.csv.CsvFileReader

@Mapper(componentModel = "spring",
        imports=[EvendiloCsvLine::class],
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
interface StandardAmazonProductMapper : CsvFileReader.CsvImportMapper<Product, StandardAmazonProductCsvLine> {
    @Component
    class Finalizer(var appConfigProperties: AppConfigurationProperties, var entityFactory: AmazonEntityFactory) {
        @AfterMapping
        fun mapAttributes(csvLine: StandardEbayProductCsvLine, @MappingTarget product: Product) {
            val (attributes, csvLineAttrs) = entityFactory.getAttributesMapping(appConfigProperties.amazon, csvLine)
        }
    }

    @Mappings(value = [
        Mapping(target = "id", ignore = true)
    ])
    override fun toNode(standardAmazonProductCsvLine: StandardAmazonProductCsvLine): Product
}