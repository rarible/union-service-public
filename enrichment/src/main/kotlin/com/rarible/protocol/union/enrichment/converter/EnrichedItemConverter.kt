package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.AmmOrdersDto
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
        val metaDto = when {
            (shortItem?.metaEntry != null) -> EnrichedMetaConverter.convert(shortItem.metaEntry)
            meta != null -> EnrichedMetaConverter.convert(meta) // TODO remove later
            (item.meta != null) -> EnrichedMetaConverter.convert(item.meta!!) // TODO remove later
            else -> null
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
            meta = metaDto,
            deleted = item.deleted,

            // Enrichment data
            bestSellOrder = shortItem?.bestSellOrder?.let { orders[it.dtoId] },
            bestBidOrder = shortItem?.bestBidOrder?.let { orders[it.dtoId] },
            originOrders = shortItem?.originOrders?.let { OriginOrdersConverter.convert(it, orders) } ?: emptyList(),
            ammOrders = AmmOrdersDto(shortItem?.poolSellOrders?.map { it.order.dtoId } ?: emptyList()),
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
