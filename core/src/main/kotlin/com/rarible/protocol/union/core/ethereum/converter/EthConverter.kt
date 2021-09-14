package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.EthAssetTypeDto
import com.rarible.protocol.union.dto.PlatformDto
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

object EthConverter {

    fun convert(address: Address) = address.prefixed()
    fun convert(word: Word) = word.prefixed()
    fun convert(binary: Binary) = binary.prefixed()

    fun convert(source: UnionActivitySortDto?): ActivitySortDto {
        return when (source) {
            null -> ActivitySortDto.LATEST_FIRST
            UnionActivitySortDto.EARLIEST_FIRST -> ActivitySortDto.EARLIEST_FIRST
            UnionActivitySortDto.LATEST_FIRST -> ActivitySortDto.LATEST_FIRST
        }
    }


    fun convert(source: PlatformDto?): com.rarible.protocol.dto.PlatformDto {
        return when (source) {
            null -> com.rarible.protocol.dto.PlatformDto.ALL
            PlatformDto.ALL -> com.rarible.protocol.dto.PlatformDto.ALL
            PlatformDto.RARIBLE -> com.rarible.protocol.dto.PlatformDto.RARIBLE
            PlatformDto.OPEN_SEA -> com.rarible.protocol.dto.PlatformDto.OPEN_SEA
        }
    }

    fun convertToPayout(source: PartDto, blockchain: EthBlockchainDto): EthOrderPayoutDto {
        return EthOrderPayoutDto(
            account = EthAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigInteger()
        )
    }

    fun convertToRoyalty(source: PartDto, blockchain: EthBlockchainDto): EthRoyaltyDto {
        return EthRoyaltyDto(
            account = EthAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigInteger()
        )
    }

    fun convertToCreator(source: PartDto, blockchain: EthBlockchainDto): EthCreatorDto {
        return EthCreatorDto(
            account = EthAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigDecimal()
        )
    }

    fun convert(source: AssetDto, blockchain: EthBlockchainDto): EthAssetDto {
        return EthAssetDto(
            type = convert(source.assetType, blockchain),
            value = source.value
        )
    }

    fun convert(source: AssetTypeDto, blockchain: EthBlockchainDto): EthAssetTypeDto {
        return when (source) {
            is com.rarible.protocol.dto.EthAssetTypeDto -> EthEthereumAssetTypeDto(
            )
            is Erc20AssetTypeDto -> EthErc20AssetTypeDto(
                contract = EthAddressConverter.convert(source.contract, blockchain)
            )
            is Erc721AssetTypeDto -> EthErc721AssetTypeDto(
                contract = EthAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId
            )
            is Erc1155AssetTypeDto -> EthErc1155AssetTypeDto(
                contract = EthAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId
            )
            is Erc721LazyAssetTypeDto -> EthErc721LazyAssetTypeDto(
                contract = EthAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId,
                uri = source.uri,
                creators = source.creators.map { convertToCreator(it, blockchain) },
                royalties = source.royalties.map { convertToRoyalty(it, blockchain) },
                signatures = source.signatures.map { convert(it) }
            )
            is Erc1155LazyAssetTypeDto -> EthErc1155LazyAssetTypeDto(
                contract = EthAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId,
                uri = source.uri,
                supply = source.supply,
                creators = source.creators.map { convertToCreator(it, blockchain) },
                royalties = source.royalties.map { convertToRoyalty(it, blockchain) },
                signatures = source.signatures.map { convert(it) }
            )
            is CryptoPunksAssetTypeDto -> EthCryptoPunksAssetTypeDto(
                contract = EthAddressConverter.convert(source.contract, blockchain),
                punkId = source.punkId
            )
            is GenerativeArtAssetTypeDto -> EthGenerativeArtAssetTypeDto(
                contract = EthAddressConverter.convert(source.contract, blockchain)
            )
        }
    }
}