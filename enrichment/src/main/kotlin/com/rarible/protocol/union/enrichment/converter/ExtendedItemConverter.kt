package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.ExtendedItemDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import java.math.BigInteger

object ExtendedItemConverter {

    fun convert(
        item: ItemDto,
        shortItem: ShortItem? = null,
        orders: Map<OrderIdDto, OrderDto> = emptyMap()
    ): ExtendedItemDto {
        return ExtendedItemDto(
            id = item.id,
            tokenId = item.tokenId,
            collection = item.collection,
            creators = item.creators,
            owners = item.owners,
            royalties = item.royalties,
            lazySupply = item.lazySupply,
            pending = item.pending,
            mintedAt = item.mintedAt,
            lastUpdatedAt = item.lastUpdatedAt,
            supply = item.supply,
            meta = item.meta,
            metaUrl = item.metaUrl,
            deleted = item.deleted,

            // Enrichment data
            unlockable = shortItem?.unlockable ?: false,
            bestSellOrder = shortItem?.bestSellOrder?.let { orders[it.dtoId] },
            bestBidOrder = shortItem?.bestBidOrder?.let { orders[it.dtoId] },
            totalStock = shortItem?.totalStock ?: BigInteger.ZERO,
            sellers = shortItem?.sellers ?: 0
        )
    }
}
