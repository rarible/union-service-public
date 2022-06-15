package com.rarible.protocol.union.enrichment.meta.cache

class ContentCacheException : RuntimeException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}