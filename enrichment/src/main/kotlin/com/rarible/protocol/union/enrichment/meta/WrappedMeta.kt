package com.rarible.protocol.union.enrichment.meta

data class WrappedMeta<T>(
    val source: MetaSource,
    val data: T
)
