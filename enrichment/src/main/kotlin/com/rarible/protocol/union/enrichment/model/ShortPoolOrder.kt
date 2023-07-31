package com.rarible.protocol.union.enrichment.model

data class ShortPoolOrder(
    val currency: String,
    val order: ShortOrder
)
