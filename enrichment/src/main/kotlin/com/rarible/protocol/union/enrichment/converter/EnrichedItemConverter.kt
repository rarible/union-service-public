package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import java.math.BigInteger

object EnrichedItemConverter {

    fun convert(
        item: UnionItem,
        shortItem: ShortItem? = null,
        meta: MetaDto? = null,
        orders: Map<OrderIdDto, OrderDto> = emptyMap(),
        auctions: List<AuctionDto> = emptyList()
    ): ItemDto {
        return ItemDto(
            id = item.id,
            blockchain = item.id.blockchain,
            contract = ContractAddress(item.id.blockchain, item.id.contract),
            collection = UnionAddressConverter.convert(item.id.blockchain, item.id.contract),
            tokenId = item.id.tokenId,
            creators = item.creators,
            owners = item.owners,
            // TODO UNION Remove in 1.19
            royalties = item.royalties,
            lazySupply = item.lazySupply,
            pending = item.pending,
            mintedAt = item.mintedAt,
            lastUpdatedAt = item.lastUpdatedAt,
            supply = item.supply,
            meta = meta,
            deleted = item.deleted,

            // Enrichment data
            bestSellOrder = shortItem?.bestSellOrder?.let { orders[it.dtoId] },
            bestBidOrder = shortItem?.bestBidOrder?.let { orders[it.dtoId] },
            auctions = auctions,
            totalStock = shortItem?.totalStock ?: BigInteger.ZERO,
            sellers = shortItem?.sellers ?: 0
        )
    }
}
