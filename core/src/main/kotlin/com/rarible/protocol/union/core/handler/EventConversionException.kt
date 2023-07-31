package com.rarible.protocol.union.core.handler

class EventConversionException(
    val event: Any?,
    cause: Throwable
) : RuntimeException(cause)
