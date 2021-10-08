package com.rarible.protocol.union.core.exception

open class UnionException : RuntimeException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}