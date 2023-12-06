package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.BlockchainDto

data class ItemChangeEvent(
    val id: ShortItemId,
    val current: ItemState?,
    val updated: ItemState,
) {
    constructor(current: ShortItem?, updated: ShortItem) : this(updated.id, current?.state(), updated.state())
}

data class ItemState(
    val blockchain: BlockchainDto,
    val bestSellOrder: ShortOrder?,
    val meta: UnionMeta?
) {
    val isListed: Boolean = bestSellOrder != null

    companion object {
        fun from(value: ShortItem): ItemState {
            return ItemState(
                blockchain = value.blockchain,
                bestSellOrder = value.bestSellOrder,
                meta = value.metaEntry?.data
            )
        }
    }
}

fun ShortItem.state(): ItemState {
    return ItemState.from(this)
}
