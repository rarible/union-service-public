package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.core.model.UnionMetaAttribute
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
    val isListed: Boolean,
    val collectionId: String?,
    val attributes: List<UnionMetaAttribute>?,
    val deleted: Boolean,
) {
    companion object {
        fun from(value: ShortItem): ItemState {
            return ItemState(
                blockchain = value.blockchain,
                isListed = value.bestSellOrder != null,
                collectionId = value.collectionId,
                attributes = value.metaEntry?.data?.attributes,
                deleted = value.deleted
            )
        }
    }
}

fun ShortItem.state(): ItemState {
    return ItemState.from(this)
}
