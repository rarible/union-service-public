package com.rarible.protocol.union.core.model

data class UnionMetaAttribute(
    val key: String,
    val value: String? = null,
    val type: String? = null,
    val format: String? = null
)