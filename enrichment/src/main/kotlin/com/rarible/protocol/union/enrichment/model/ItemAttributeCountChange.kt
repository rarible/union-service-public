package com.rarible.protocol.union.enrichment.model

data class ItemAttributeCountChange(
    val attribute: ItemAttributeShort,
    val totalChange: Long,
    val listedChange: Long,
) {
    override fun toString(): String {
        return "attribute={$attribute},total=$totalChange,listed=$listedChange)"
    }
}
