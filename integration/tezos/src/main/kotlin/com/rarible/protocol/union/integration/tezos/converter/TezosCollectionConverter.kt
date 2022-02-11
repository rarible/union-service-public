package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.tezos.dto.NftCollectionDto
import com.rarible.protocol.tezos.dto.NftCollectionFeatureDto
import com.rarible.protocol.tezos.dto.NftCollectionTypeDto
import com.rarible.protocol.tezos.dto.NftCollectionsDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import org.slf4j.LoggerFactory

object TezosCollectionConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(source: NftCollectionDto, blockchain: BlockchainDto): CollectionDto {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Collection: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private fun convertInternal(source: NftCollectionDto, blockchain: BlockchainDto): CollectionDto {
        return CollectionDto(
            id = CollectionIdDto(blockchain, source.id),
            blockchain = blockchain,
            name = source.name,
            symbol = source.symbol,
            owner = source.owner?.let { UnionAddressConverter.convert(blockchain, it) },
            type = convert(source.type),
            features = source.features.map { convert(it) },
            minters = source.minters?.let { minters -> minters.map { UnionAddressConverter.convert(blockchain, it) } }
        )
    }

    fun convert(page: NftCollectionsDto, blockchain: BlockchainDto): Page<CollectionDto> {
        return Page(
            total = page.total.toLong(),
            continuation = page.continuation,
            entities = page.collections.map { convert(it, blockchain) }
        )
    }

    private fun convert(feature: NftCollectionFeatureDto): CollectionDto.Features {
        return when (feature) {
            NftCollectionFeatureDto.APPROVE_FOR_ALL -> CollectionDto.Features.APPROVE_FOR_ALL
            NftCollectionFeatureDto.BURN -> CollectionDto.Features.BURN
            NftCollectionFeatureDto.MINT_AND_TRANSFER -> CollectionDto.Features.MINT_AND_TRANSFER
            NftCollectionFeatureDto.MINT_WITH_ADDRESS -> CollectionDto.Features.MINT_WITH_ADDRESS
            NftCollectionFeatureDto.SECONDARY_SALE_FEES -> CollectionDto.Features.SECONDARY_SALE_FEES
            NftCollectionFeatureDto.SET_URI_PREFIX -> CollectionDto.Features.SET_URI_PREFIX
        }
    }

    private fun convert(type: NftCollectionTypeDto): CollectionDto.Type {
        return when (type) {
            NftCollectionTypeDto.NFT -> CollectionDto.Type.TEZOS_NFT
            NftCollectionTypeDto.MT -> CollectionDto.Type.TEZOS_MT
        }
    }

}
