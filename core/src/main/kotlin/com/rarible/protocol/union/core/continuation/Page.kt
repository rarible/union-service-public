package com.rarible.protocol.union.core.continuation

data class Page<T>(
    val total: Long,
    val continuation: String?,
    val entities: List<T>
)