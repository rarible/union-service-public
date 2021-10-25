package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto

object EthCollectionConverter {

    fun convert(source: NftCollectionDto, blockchain: BlockchainDto): CollectionDto {
        return CollectionDto(
            id = EthConverter.convert(source.id, blockchain),
            name = source.name,
            symbol = source.symbol,
            type = convert(source.type),
            owner = source.owner?.let { EthConverter.convert(it, blockchain) },
            features = source.features.map { convert(it) }
        )
    }

    fun convert(page: NftCollectionsDto, blockchain: BlockchainDto): Page<CollectionDto> {
        return Page(
            total = page.total,
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

}

