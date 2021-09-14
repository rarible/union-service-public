package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.EthCollectionDto
import com.rarible.protocol.union.dto.UnionCollectionsDto

object EthUnionCollectionConverter {

    fun convert(source: NftCollectionDto, blockchain: EthBlockchainDto): EthCollectionDto {
        return EthCollectionDto(
            id = EthAddressConverter.convert(source.id, blockchain),
            name = source.name,
            symbol = source.symbol,
            type = convert(source.type),
            supportsLazyMint = source.supportsLazyMint,
            owner = if (source.owner == null) null else EthAddressConverter.convert(
                source.owner!!,
                blockchain
            ),
            features = source.features.map { convert(it) }
        )
    }

    fun convert(page: NftCollectionsDto, blockchain: EthBlockchainDto): UnionCollectionsDto {
        return UnionCollectionsDto(
            total = page.total,
            continuation = page.continuation,
            collections = page.collections.map { convert(it, blockchain) }
        )
    }

    private fun convert(type: NftCollectionDto.Type): EthCollectionDto.Type {
        return when (type) {
            NftCollectionDto.Type.ERC721 -> EthCollectionDto.Type.ERC721
            NftCollectionDto.Type.ERC1155 -> EthCollectionDto.Type.ERC1155
            NftCollectionDto.Type.CRYPTO_PUNKS -> EthCollectionDto.Type.CRYPTO_PUNKS
        }
    }

    private fun convert(feature: NftCollectionDto.Features): EthCollectionDto.Features {
        return when (feature) {
            NftCollectionDto.Features.APPROVE_FOR_ALL -> EthCollectionDto.Features.APPROVE_FOR_ALL
            NftCollectionDto.Features.BURN -> EthCollectionDto.Features.BURN
            NftCollectionDto.Features.MINT_AND_TRANSFER -> EthCollectionDto.Features.MINT_AND_TRANSFER
            NftCollectionDto.Features.MINT_WITH_ADDRESS -> EthCollectionDto.Features.MINT_WITH_ADDRESS
            NftCollectionDto.Features.SECONDARY_SALE_FEES -> EthCollectionDto.Features.SECONDARY_SALE_FEES
            NftCollectionDto.Features.SET_URI_PREFIX -> EthCollectionDto.Features.SET_URI_PREFIX
        }
    }

}

