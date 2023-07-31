package com.rarible.protocol.union.core.exception

/**
 * Exception for integration errors for cases when data provided by blockchains is incorrect
 */
open class UnionDataFormatException : RuntimeException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
