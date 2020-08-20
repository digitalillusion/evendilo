package xyz.deverse.evendilo.importer

enum class ErrorCode {
    IMPORT_LINE_ERROR_PRODUCT_TYPE,
    IMPORT_LINE_ERROR_PRODUCT_CATEGORY,
    IMPORT_LINE_ERROR_PRODUCT_TAG
}

class ImportLineException(errorCode: ErrorCode) : RuntimeException(errorCode.toString())