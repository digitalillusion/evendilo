package xyz.deverse.evendilo.importer.standard.amazon.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import xyz.deverse.evendilo.importer.standard.amazon.StandardAmazonProductCsvLine
import xyz.deverse.evendilo.model.amazon.Product
import xyz.deverse.evendilo.model.woocommerce.Attribute
import xyz.deverse.evendilo.model.woocommerce.Category
import xyz.deverse.evendilo.model.woocommerce.Image
import xyz.deverse.evendilo.model.woocommerce.ProductType
import xyz.deverse.importer.csv.CsvFileReader

@Mapper(componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
interface StandardAmazonProductMapper : CsvFileReader.CsvImportMapper<Product, StandardAmazonProductCsvLine> {

}