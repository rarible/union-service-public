package com.rarible.protocol.union.api.continuation

data class ContinuationPage<T, C>(
    val entities: List<T>,
    val continuation: C?
)