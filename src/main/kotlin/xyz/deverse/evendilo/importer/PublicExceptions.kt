package xyz.deverse.evendilo.importer

enum class ErrorCode {
    IMPORT_LINE_ERROR_WRONG_CATEGORY
}

class ImportLineException(errorCode: ErrorCode) : RuntimeException(errorCode.toString())