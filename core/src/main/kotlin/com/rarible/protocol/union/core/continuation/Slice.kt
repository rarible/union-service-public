package com.rarible.protocol.union.core.continuation

data class Slice<T>(
    val continuation: String?,
    val entities: List<T>
)