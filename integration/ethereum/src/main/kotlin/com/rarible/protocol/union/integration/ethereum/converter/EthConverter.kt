package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.CollectionAssetTypeDto
import com.rarible.protocol.dto.CryptoPunksAssetTypeDto
import com.rarible.protocol.dto.Erc1155AssetTypeDto
import com.rarible.protocol.dto.Erc1155LazyAssetTypeDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.dto.Erc721LazyAssetTypeDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.GenerativeArtAssetTypeDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.EthCollectionAssetTypeDto
import com.rarible.protocol.union.dto.EthCryptoPunksAssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155AssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.EthGenerativeArtAssetTypeDto
import com.rarible.protocol.union.dto.OrderPayoutDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.UnionAddress
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

object EthConverter {

    fun convert(address: Address) = address.prefixed()!!
    fun convert(word: Word) = word.prefixed()!!
    fun convert(binary: Binary) = binary.prefixed()!!

    fun convertToWord(value: String): Word {
        try {
            return Word.apply(value)!!
        } catch (e: Throwable) {
            throw UnionValidationException("Incorrect Ethereum word format: $value")
        }
    }

    fun convertToAddress(value: String): Address {
        try {
            return Address.apply(value)!!
        } catch (e: Throwable) {
            throw UnionValidationException("Incorrect Ethereum address format: $value")
        }
    }

    fun convertToBinary(value: String): Binary {
        try {
            return Binary.apply(value)!!
        } catch (e: Throwable) {
            throw UnionValidationException("Incorrect Ethereum binary format: $value")
        }
    }

    fun convert(source: Address, blockchain: BlockchainDto): UnionAddress {
        return UnionAddressConverter.convert(blockchain, convert(source))
    }

    fun convert(source: ActivitySortDto?): com.rarible.protocol.dto.ActivitySortDto {
        return when (source) {
            null -> com.rarible.protocol.dto.ActivitySortDto.LATEST_FIRST
            ActivitySortDto.EARLIEST_FIRST -> com.rarible.protocol.dto.ActivitySortDto.EARLIEST_FIRST
            ActivitySortDto.LATEST_FIRST -> com.rarible.protocol.dto.ActivitySortDto.LATEST_FIRST
        }
    }

    fun convert(source: PlatformDto?): com.rarible.protocol.dto.PlatformDto {
        return when (source) {
            null -> com.rarible.protocol.dto.PlatformDto.ALL
            PlatformDto.ALL -> com.rarible.protocol.dto.PlatformDto.ALL
            PlatformDto.RARIBLE -> com.rarible.protocol.dto.PlatformDto.RARIBLE
            PlatformDto.OPEN_SEA -> com.rarible.protocol.dto.PlatformDto.OPEN_SEA
            PlatformDto.CRYPTO_PUNKS -> com.rarible.protocol.dto.PlatformDto.CRYPTO_PUNKS
        }
    }

    fun convertToPayout(source: PartDto, blockchain: BlockchainDto): OrderPayoutDto {
        return OrderPayoutDto(
            account = convert(source.account, blockchain),
            value = source.value
        )
    }

    fun convertToRoyalty(source: PartDto, blockchain: BlockchainDto): RoyaltyDto {
        return RoyaltyDto(
            account = convert(source.account, blockchain),
            value = source.value
        )
    }

    fun convertToCreator(source: PartDto, blockchain: BlockchainDto): CreatorDto {
        return CreatorDto(
            account = convert(source.account, blockchain),
            value = source.value
        )
    }

    fun convert(source: com.rarible.protocol.dto.AssetDto, blockchain: BlockchainDto): AssetDto {
        return AssetDto(
            type = convert(source.assetType, blockchain),
            value = source.valueDecimal!!
        )
    }

    fun convert(source: com.rarible.protocol.dto.AssetTypeDto, blockchain: BlockchainDto): AssetTypeDto {
        return when (source) {
            is EthAssetTypeDto -> EthEthereumAssetTypeDto()
            is Erc20AssetTypeDto -> EthErc20AssetTypeDto(
                contract = convert(source.contract, blockchain)
            )
            is Erc721AssetTypeDto -> EthErc721AssetTypeDto(
                contract = convert(source.contract, blockchain),
                tokenId = source.tokenId
            )
            is Erc1155AssetTypeDto -> EthErc1155AssetTypeDto(
                contract = convert(source.contract, blockchain),
                tokenId = source.tokenId
            )
            is Erc721LazyAssetTypeDto -> EthErc721LazyAssetTypeDto(
                contract = convert(source.contract, blockchain),
                tokenId = source.tokenId,
                uri = source.uri,
                creators = source.creators.map { convertToCreator(it, blockchain) },
                royalties = source.royalties.map { convertToRoyalty(it, blockchain) },
                signatures = source.signatures.map { convert(it) }
            )
            is Erc1155LazyAssetTypeDto -> EthErc1155LazyAssetTypeDto(
                contract = convert(source.contract, blockchain),
                tokenId = source.tokenId,
                uri = source.uri,
                supply = source.supply,
                creators = source.creators.map { convertToCreator(it, blockchain) },
                royalties = source.royalties.map { convertToRoyalty(it, blockchain) },
                signatures = source.signatures.map { convert(it) }
            )
            is CryptoPunksAssetTypeDto -> EthCryptoPunksAssetTypeDto(
                contract = convert(source.contract, blockchain),
                punkId = source.punkId
            )
            is GenerativeArtAssetTypeDto -> EthGenerativeArtAssetTypeDto(
                contract = convert(source.contract, blockchain)
            )
            is CollectionAssetTypeDto -> EthCollectionAssetTypeDto(
                contract = convert(source.contract, blockchain)
            )
        }
    }
}
