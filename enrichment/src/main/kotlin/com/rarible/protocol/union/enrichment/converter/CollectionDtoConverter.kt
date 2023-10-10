package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import org.slf4j.LoggerFactory

object CollectionDtoConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(
        collection: EnrichmentCollection,
        meta: UnionCollectionMeta? = null,
        orders: Map<OrderIdDto, OrderDto> = emptyMap(),
    ): CollectionDto {
        return CollectionDto(
            id = collection.id.toDto(),
            blockchain = collection.id.blockchain,
            features = collection.features.map { convert(it) },
            owner = collection.owner,
            minters = collection.minters,
            name = collection.name?.ifBlank { meta?.name } ?: meta?.name ?: "Untitled",
            status = collection.status?.let { convert(it) },
            symbol = collection.symbol,
            parent = collection.parent?.toDto(),
            structure = convert(collection.structure!!), // TODO Must be required after the migration
            type = convert(collection.id, collection.type), // TODO Must be required after the migration
            self = collection.self,
            scam = collection.scam,
            meta = meta?.let { MetaDtoConverter.convert(it) },
            bestSellOrder = collection.bestSellOrder?.let { orders[it.dtoId] },
            bestBidOrder = collection.bestBidOrder?.let { orders[it.dtoId] },
            originOrders = OriginOrdersConverter.convert(collection.originOrders, orders),
            bestBidOrdersByCurrency = collection.bestBidOrders.values
                .filter { it.id != collection.bestBidOrder?.id }
                .mapNotNull { orders[it.dtoId] }.ifEmpty { null },
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
            UnionCollection.Features.PAUSABLE -> CollectionDto.Features.PAUSABLE
            UnionCollection.Features.NOT_FOR_SALE -> CollectionDto.Features.NOT_FOR_SALE
        }
    }

    private fun convert(structure: UnionCollection.Structure): CollectionDto.Structure {
        return when (structure) {
            UnionCollection.Structure.REGULAR -> CollectionDto.Structure.REGULAR
            UnionCollection.Structure.COMPOSITE -> CollectionDto.Structure.COMPOSITE
            UnionCollection.Structure.PART -> CollectionDto.Structure.PART
        }
    }

    private fun convert(collectionId: EnrichmentCollectionId, source: UnionCollection.Type?): CollectionDto.Type {
        // TODO workaround for 'ghost' collections remain in DB after the migration to Union DB,
        // should be fixed later, now just watch the logs
        return if (source == null) {
            logger.warn("Unknown collection {}, type can't be determined", collectionId.toDto().fullId())
            when (collectionId.blockchain) {
                BlockchainDto.ETHEREUM,
                BlockchainDto.POLYGON,
                BlockchainDto.MANTLE -> CollectionDto.Type.ERC721
                BlockchainDto.IMMUTABLEX -> CollectionDto.Type.IMMUTABLEX
                BlockchainDto.FLOW -> CollectionDto.Type.FLOW
                BlockchainDto.TEZOS -> CollectionDto.Type.TEZOS_NFT
                BlockchainDto.SOLANA -> CollectionDto.Type.SOLANA
            }
        } else {
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
    }

    private fun convert(source: UnionCollection.Status): CollectionDto.Status {
        return when (source) {
            UnionCollection.Status.CONFIRMED -> CollectionDto.Status.CONFIRMED
            UnionCollection.Status.PENDING -> CollectionDto.Status.PENDING
            UnionCollection.Status.ERROR -> CollectionDto.Status.ERROR
        }
    }
}
