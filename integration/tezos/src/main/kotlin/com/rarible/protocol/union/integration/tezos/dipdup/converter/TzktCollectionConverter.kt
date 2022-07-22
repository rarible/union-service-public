package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.entity.TezosCollection
import com.rarible.tzkt.model.CollectionType
import com.rarible.tzkt.model.Contract
import org.slf4j.LoggerFactory

object TzktCollectionConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(source: List<Contract>, blockchain: BlockchainDto): List<UnionCollection> {
        return source.map { convert(it, blockchain) }
    }

    fun convert(source: Contract, blockchain: BlockchainDto): UnionCollection {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Collection: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    fun convert(tzktPage: com.rarible.tzkt.model.Page<Contract>, blockchain: BlockchainDto): Page<UnionCollection> {
        return Page(
            total = tzktPage.items.size.toLong(),
            continuation = tzktPage.continuation,
            entities = tzktPage.items.map { convertInternal(it, blockchain) }
        )
    }

    fun convertType(source: TezosCollection.Type?): CollectionType {
        return when (source) {
            TezosCollection.Type.NFT -> CollectionType.NFT
            else -> CollectionType.MT
        }
    }

    fun convertType(source: CollectionType): TezosCollection.Type {
        return when (source) {
            CollectionType.NFT -> TezosCollection.Type.NFT
            else -> TezosCollection.Type.MT
        }
    }

    private fun convertInternal(source: Contract, blockchain: BlockchainDto): UnionCollection {
        return UnionCollection(
            id = CollectionIdDto(
                blockchain,
                source.address!!
            ), // In model all fields are nullable but in fact they aren't
            name = source.name ?: "Unnamed Collection",
            symbol = source.symbol,
            owner = owner(source, blockchain),
            type = convertType(source),
            features = features(source),
            minters = minters(source, blockchain)
        )
    }

    private fun convertType(source: Contract): CollectionDto.Type {
        return when {
            source.collectionType == CollectionType.NFT -> CollectionDto.Type.TEZOS_NFT
            else -> CollectionDto.Type.TEZOS_MT
        }
    }

    private fun owner(source: Contract, blockchain: BlockchainDto): UnionAddress? {
        return source.creator?.let { UnionAddressConverter.convert(blockchain, it.address!!) }
    }

    private fun features(source: Contract): List<CollectionDto.Features> {
        return when(convertType(source)) {
            CollectionDto.Type.TEZOS_MT -> listOf(CollectionDto.Features.SECONDARY_SALE_FEES, CollectionDto.Features.BURN)
            else -> emptyList()
        }
    }

    private fun minters(source: Contract, blockchain: BlockchainDto): List<UnionAddress>? {
        return owner(source, blockchain)?.let { listOf(it) }
    }
}
