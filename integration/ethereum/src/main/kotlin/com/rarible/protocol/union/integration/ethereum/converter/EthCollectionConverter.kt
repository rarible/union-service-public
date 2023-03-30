package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.dto.NftTokenIdDto
import com.rarible.protocol.union.core.model.TokenId
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import org.slf4j.LoggerFactory

object EthCollectionConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(source: NftCollectionDto, blockchain: BlockchainDto): UnionCollection {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Collection: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    fun convert(source: NftCollectionEventDto, blockchain: BlockchainDto): UnionCollectionEvent {
        return when (source) {
            is NftCollectionUpdateEventDto -> {
                UnionCollectionUpdateEvent(
                    collection = convert(source.collection, blockchain),
                    eventTimeMarks = EthConverter.convert(source.eventTimeMarks)
                )
            }
        }
    }

    private fun convertInternal(source: NftCollectionDto, blockchain: BlockchainDto): UnionCollection {
        val contract = EthConverter.convert(source.id)
        return UnionCollection(
            id = CollectionIdDto(blockchain, contract),
            name = source.name,
            status = convert(source.status),
            symbol = source.symbol,
            type = convert(source.type),
            owner = source.owner?.let { EthConverter.convert(it, blockchain) },
            features = source.features.map { convert(it) },
            minters = source.minters?.let { minters -> minters.map { EthConverter.convert(it, blockchain) } },
            meta = source.meta?.let { EthMetaConverter.convert(it, blockchain) },
            self = source.isRaribleContract
        )
    }

    private fun convert(status: NftCollectionDto.Status?): CollectionDto.Status? {
        return when (status) {
            NftCollectionDto.Status.PENDING -> CollectionDto.Status.PENDING
            NftCollectionDto.Status.ERROR -> CollectionDto.Status.ERROR
            NftCollectionDto.Status.CONFIRMED -> CollectionDto.Status.CONFIRMED
            null -> null
        }
    }

    fun convert(page: NftCollectionsDto, blockchain: BlockchainDto): Page<UnionCollection> {
        return Page(
            total = page.total ?: 0,
            continuation = page.continuation,
            entities = page.collections.map { convert(it, blockchain) }
        )
    }

    private fun convert(type: NftCollectionDto.Type): CollectionDto.Type {
        return when (type) {
            NftCollectionDto.Type.ERC721 -> CollectionDto.Type.ERC721
            NftCollectionDto.Type.ERC1155 -> CollectionDto.Type.ERC1155
            NftCollectionDto.Type.CRYPTO_PUNKS -> CollectionDto.Type.CRYPTO_PUNKS
        }
    }

    private fun convert(feature: NftCollectionDto.Features): CollectionDto.Features {
        return when (feature) {
            NftCollectionDto.Features.APPROVE_FOR_ALL -> CollectionDto.Features.APPROVE_FOR_ALL
            NftCollectionDto.Features.BURN -> CollectionDto.Features.BURN
            NftCollectionDto.Features.MINT_AND_TRANSFER -> CollectionDto.Features.MINT_AND_TRANSFER
            NftCollectionDto.Features.MINT_WITH_ADDRESS -> CollectionDto.Features.MINT_WITH_ADDRESS
            NftCollectionDto.Features.SECONDARY_SALE_FEES -> CollectionDto.Features.SECONDARY_SALE_FEES
            NftCollectionDto.Features.SET_URI_PREFIX -> CollectionDto.Features.SET_URI_PREFIX
        }
    }

    fun convert(source: NftTokenIdDto) = TokenId(source.tokenId.toString())
}
