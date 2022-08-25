package com.rarible.protocol.union.core.exception

class UnionMetaException : UnionException {
    val code: ErrorCode

    constructor(code: ErrorCode, message: String?) : super(message) {
        this.code = code
    }

    enum class ErrorCode {
        UNPARSEABLE_JSON,
        UNPARSEABLE_LINK,
        TIMEOUT,
        UNKNOWN
    }
}