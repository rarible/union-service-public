package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionCollectionDto
import com.rarible.protocol.union.dto.UnionCollectionsDto

object EthUnionCollectionConverter {

    fun convert(source: NftCollectionDto, blockchain: BlockchainDto): UnionCollectionDto {
        return UnionCollectionDto(
            id = UnionAddressConverter.convert(source.id, blockchain),
            name = source.name,
            symbol = source.symbol,
            type = convert(source.type),
            owner = if (source.owner == null) null else UnionAddressConverter.convert(
                source.owner!!,
                blockchain
            ),
            features = source.features.map { convert(it) }
        )
    }

    fun convert(page: NftCollectionsDto, blockchain: BlockchainDto): UnionCollectionsDto {
        return UnionCollectionsDto(
            total = page.total,
            continuation = page.continuation,
            collections = page.collections.map { convert(it, blockchain) }
        )
    }

    private fun convert(type: NftCollectionDto.Type): UnionCollectionDto.Type {
        return when (type) {
            NftCollectionDto.Type.ERC721 -> UnionCollectionDto.Type.ERC721
            NftCollectionDto.Type.ERC1155 -> UnionCollectionDto.Type.ERC1155
            NftCollectionDto.Type.CRYPTO_PUNKS -> UnionCollectionDto.Type.CRYPTO_PUNKS
        }
    }

    private fun convert(feature: NftCollectionDto.Features): UnionCollectionDto.Features {
        return when (feature) {
            NftCollectionDto.Features.APPROVE_FOR_ALL -> UnionCollectionDto.Features.APPROVE_FOR_ALL
            NftCollectionDto.Features.BURN -> UnionCollectionDto.Features.BURN
            NftCollectionDto.Features.MINT_AND_TRANSFER -> UnionCollectionDto.Features.MINT_AND_TRANSFER
            NftCollectionDto.Features.MINT_WITH_ADDRESS -> UnionCollectionDto.Features.MINT_WITH_ADDRESS
            NftCollectionDto.Features.SECONDARY_SALE_FEES -> UnionCollectionDto.Features.SECONDARY_SALE_FEES
            NftCollectionDto.Features.SET_URI_PREFIX -> UnionCollectionDto.Features.SET_URI_PREFIX
        }
    }

}

