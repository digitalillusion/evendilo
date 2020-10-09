package xyz.deverse.evendilo.importer.standard

import org.springframework.stereotype.Component
import xyz.deverse.evendilo.importer.business.EntityFactory
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Model
import xyz.deverse.importer.csv.CsvColumn
import xyz.deverse.importer.csv.CsvFileReader


open class EvendiloCsvLine<T> (
        @CsvColumn(0) var timestamp: String = "",
        @CsvColumn(11) var attr0: String = "",
        @CsvColumn(12) var attr1: String = "",
        @CsvColumn(13) var attr2: String = "",
        @CsvColumn(14) var attr3: String = "",
        @CsvColumn(15) var attr4: String = "",
        @CsvColumn(16) var attr5: String = "",
        @CsvColumn(17) var attr6: String = "",
        @CsvColumn(18) var attr7: String = "",
        @CsvColumn(19) var attr8: String = "",
        @CsvColumn(20) var attr9: String = ""
) : CsvFileReader.CsvLine<T>()

@Component
class WoocommerceEntityFactory : EntityFactory(Destination.Woocommerce) {
    override fun <T : Model> populateOnCreate(node: T) {
        // NOOP
    }

}

@Component
class EbayEntityFactory : EntityFactory(Destination.Ebay) {
    override fun <T : Model> populateOnCreate(node: T) {
        // NOOP
    }

}

@Component
class AmazonEntityFactory : EntityFactory(Destination.Amazon) {
    override fun <T : Model> populateOnCreate(node: T) {
        // NOOP
    }

}