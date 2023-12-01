package com.rarible.protocol.union.enrichment.model

data class ItemAttributeCountChange(
    val attribute: ItemAttributeShort,
    val totalChange: Long,
    val listedChange: Long,
)
