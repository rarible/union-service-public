package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection

object CollectionDtoConverter {

    fun convert(
        // TODO COLLECTION won't be needed after the migration
        collection: UnionCollection,
        // TODO COLLECTION must be required after the migration
        enrichmentCollection: EnrichmentCollection? = null,
        meta: UnionCollectionMeta? = null,
        orders: Map<OrderIdDto, OrderDto> = emptyMap()
    ): CollectionDto {
        return CollectionDto(
            id = collection.id,
            blockchain = collection.id.blockchain,
            features = collection.features.map { convert(it) },
            owner = collection.owner,
            minters = collection.minters,
            name = collection.name,
            status = collection.status?.let { convert(it) },
            symbol = collection.symbol,
            parent = collection.parent,
            meta = (meta ?: collection.meta)?.let { MetaDtoConverter.convert(it) },
            type = convert(collection.type),
            self = collection.self,
            bestSellOrder = enrichmentCollection?.bestSellOrder?.let { orders[it.dtoId] },
            bestBidOrder = enrichmentCollection?.bestBidOrder?.let { orders[it.dtoId] },
            originOrders = enrichmentCollection?.originOrders?.let { OriginOrdersConverter.convert(it, orders) }
                ?: emptyList()
        )
    }

    private fun convert(source: UnionCollection.Features): CollectionDto.Features {
        return when (source) {
            UnionCollection.Features.APPROVE_FOR_ALL -> CollectionDto.Features.APPROVE_FOR_ALL
            UnionCollection.Features.SET_URI_PREFIX -> CollectionDto.Features.SET_URI_PREFIX
            UnionCollection.Features.BURN -> CollectionDto.Features.BURN
            UnionCollection.Features.MINT_WITH_ADDRESS -> CollectionDto.Features.MINT_WITH_ADDRESS
            UnionCollection.Features.SECONDARY_SALE_FEES -> CollectionDto.Features.SECONDARY_SALE_FEES
            UnionCollection.Features.MINT_AND_TRANSFER -> CollectionDto.Features.MINT_AND_TRANSFER
        }
    }

    private fun convert(source: UnionCollection.Type): CollectionDto.Type {
        return when (source) {
            UnionCollection.Type.CRYPTO_PUNKS -> CollectionDto.Type.CRYPTO_PUNKS
            UnionCollection.Type.ERC721 -> CollectionDto.Type.ERC721
            UnionCollection.Type.ERC1155 -> CollectionDto.Type.ERC1155
            UnionCollection.Type.FLOW -> CollectionDto.Type.FLOW
            UnionCollection.Type.TEZOS_NFT -> CollectionDto.Type.TEZOS_NFT
            UnionCollection.Type.TEZOS_MT -> CollectionDto.Type.TEZOS_MT
            UnionCollection.Type.SOLANA -> CollectionDto.Type.SOLANA
            UnionCollection.Type.IMMUTABLEX -> CollectionDto.Type.IMMUTABLEX
        }
    }

    private fun convert(source: UnionCollection.Status): CollectionDto.Status {
        return when (source) {
            UnionCollection.Status.CONFIRMED -> CollectionDto.Status.CONFIRMED
            UnionCollection.Status.PENDING -> CollectionDto.Status.PENDING
            UnionCollection.Status.ERROR -> CollectionDto.Status.ERROR
        }
    }

}
