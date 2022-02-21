package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import java.math.BigInteger

object EnrichedItemConverter {

    fun convert(
        item: UnionItem,
        shortItem: ShortItem? = null,
        meta: UnionMeta? = null,
        orders: Map<OrderIdDto, OrderDto> = emptyMap(),
        auctions: Map<AuctionIdDto, AuctionDto> = emptyMap()
    ): ItemDto {
        val contractAndTokenId = if (item.id.blockchain != BlockchainDto.SOLANA) {
            CompositeItemIdParser.split(item.id.value)
        } else {
            null
        }
        return ItemDto(
            id = item.id,
            blockchain = item.id.blockchain,
            collection = item.collection,
            contract = contractAndTokenId?.let { ContractAddressConverter.convert(item.id.blockchain, it.first) }, // TODO remove later
            tokenId = contractAndTokenId?.second, // TODO remove later
            creators = item.creators,
            owners = item.owners, // TODO UNION Remove in 1.19
            royalties = item.royalties, // TODO UNION Remove in 1.19
            lazySupply = item.lazySupply,
            pending = item.pending,
            mintedAt = item.mintedAt,
            lastUpdatedAt = item.lastUpdatedAt,
            supply = item.supply,
            meta = meta?.let { EnrichedMetaConverter.convert(it) },
            deleted = item.deleted,

            // Enrichment data
            bestSellOrder = shortItem?.bestSellOrder?.let { orders[it.dtoId] },
            bestBidOrder = shortItem?.bestBidOrder?.let { orders[it.dtoId] },
            auctions = shortItem?.auctions?.mapNotNull { auctions[it] } ?: emptyList(),
            totalStock = shortItem?.totalStock ?: BigInteger.ZERO,
            sellers = shortItem?.sellers ?: 0
        )
    }
}
