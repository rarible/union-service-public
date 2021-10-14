package com.rarible.protocol.union.enrichment.meta

data class ContentMeta(
    val type: String,
    val width: Int? = null,
    val height: Int? = null,
    val size: Long? = null
)
