package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.core.model.UnionMeta

data class ItemChangeEvent(
    val id: ShortItemId,
    val current: ItemState?,
    val updated: ItemState,
)

data class ItemState(
    val bestSellOrder: ShortOrder?,
    val meta: UnionMeta?
) {
    val isListed: Boolean = bestSellOrder != null

    companion object {
        fun from(value: ShortItem): ItemState {
            return ItemState(
                bestSellOrder = value.bestSellOrder,
                meta = value.metaEntry?.data
            )
        }
    }
}

fun ShortItem.state(): ItemState {
    return ItemState.from(this)
}
