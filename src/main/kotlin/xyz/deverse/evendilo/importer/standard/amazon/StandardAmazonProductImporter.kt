package xyz.deverse.evendilo.importer.standard.amazon

import org.springframework.stereotype.Service
import xyz.deverse.evendilo.importer.standard.EvendiloCsvLine
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.amazon.Product
import xyz.deverse.importer.AbstractImporter
import xyz.deverse.importer.ImportMapper
import xyz.deverse.importer.csv.CsvColumn

data class StandardAmazonProductCsvLine(
        @CsvColumn(1) var sku: String = "",
        @CsvColumn(2) var name: String = "",
        @CsvColumn(3) var type: String = "",
        @CsvColumn(4) var sale_price: String = "",
        @CsvColumn(5) var regular_price: String = "",
        @CsvColumn(6) var short_description: String = "",
        @CsvColumn(7) var description: String = "",
        @CsvColumn(8) var categoryNames: String = "",
        @CsvColumn(9) var tagNames: String = "",
        @CsvColumn(10) var imageUrls: String = ""
) : EvendiloCsvLine<Product>()

@Service
class StandardAmazonProductImporter() :
        AbstractImporter<Product, ImportMapper.MappedLine<Product>>(Family.Standard, Destination.Amazon) {

    override fun preProcess() {
    }

    override fun onParseLine(line: ImportMapper.MappedLine<Product>) {
    }

}