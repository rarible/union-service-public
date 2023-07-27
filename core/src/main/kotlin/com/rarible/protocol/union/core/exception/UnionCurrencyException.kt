package com.rarible.protocol.union.core.exception

class UnionCurrencyException : UnionException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
