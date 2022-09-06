package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemLastSaleDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.model.ItemLastSale
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
            contract = contractAndTokenId?.let { // TODO remove later
                ContractAddressConverter.convert(item.id.blockchain, it.first)
            },
            tokenId = contractAndTokenId?.second, // TODO remove later
            creators = item.creators,
            lazySupply = item.lazySupply,
            pending = item.pending,
            mintedAt = item.mintedAt,
            lastUpdatedAt = item.lastUpdatedAt,
            supply = item.supply,
            // TODO: see CHARLIE-158: we will ignore meta from blockhain Item DTOs' soon and only load metadata on union.
            //  This fallback is needed to guarantee that the first event for a just minted item contains meta.
            meta = (meta ?: item.meta)?.let { EnrichedMetaConverter.convert(it) },
            deleted = item.deleted,

            // Enrichment data
            bestSellOrder = shortItem?.bestSellOrder?.let { orders[it.dtoId] },
            bestBidOrder = shortItem?.bestBidOrder?.let { orders[it.dtoId] },
            originOrders = shortItem?.originOrders?.let { OriginOrdersConverter.convert(it, orders) } ?: emptyList(),
            ammOrders = null,
            auctions = shortItem?.auctions?.mapNotNull { auctions[it] } ?: emptyList(),
            totalStock = shortItem?.totalStock ?: BigInteger.ZERO,
            sellers = shortItem?.sellers ?: 0,
            lastSale = shortItem?.lastSale?.let { convert(it) }
        )
    }

    private fun convert(lastSale: ItemLastSale): ItemLastSaleDto {
        return ItemLastSaleDto(
            date = lastSale.date,
            seller = lastSale.seller,
            buyer = lastSale.buyer,
            currency = lastSale.currency,
            value = lastSale.value,
            price = lastSale.price
        )
    }

}
