package com.rarible.protocol.union.core.exception

class UnionMetaException : UnionException {
    val code: ErrorCode

    constructor(code: ErrorCode, message: String?) : super(message) {
        this.code = code
    }

    enum class ErrorCode {
        NOT_FOUND,
        CORRUPTED_DATA,
        CORRUPTED_URL,
        TIMEOUT,
        ERROR
    }
}
