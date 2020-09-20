package xyz.deverse.evendilo.model;

import com.fasterxml.jackson.annotation.JsonValue
import xyz.deverse.evendilo.importer.ErrorCode
import xyz.deverse.evendilo.importer.ImportLineException

enum class ProductType {
    Simple, Grouped, External, Variable, Variation;

    @JsonValue
    override fun toString(): String {
        return super.toString().toLowerCase()
    }

    companion object {
        @JvmStatic
        fun fromString(type: String): ProductType {
            return when (type.toLowerCase()) {
                Simple.toString() -> Simple
                Grouped.toString() -> Grouped
                External.toString() -> External
                Variable.toString() -> Variable
                Variation.toString() -> Variation
                else -> throw ImportLineException(ErrorCode.IMPORT_LINE_ERROR_PRODUCT_TYPE)
            }
        }
    }
}