package xyz.deverse.evendilo.importer.standard.amazon

import org.springframework.stereotype.Service
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.amazon.Product
import xyz.deverse.importer.AbstractImporter
import xyz.deverse.importer.ImportMapper
import xyz.deverse.importer.csv.CsvColumn
import xyz.deverse.importer.csv.CsvFileReader

data class StandardAmazonProductCsvLine(
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
class StandardAmazonProductImporter() :
        AbstractImporter<Product, ImportMapper.MappedLine<Product>>(Family.Standard, Destination.Amazon) {

    override fun preProcess() {
    }

    override fun onParseLine(line: ImportMapper.MappedLine<Product>) {
    }

}