package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.AssetDto
import com.rarible.protocol.dto.AssetTypeDto
import com.rarible.protocol.dto.CryptoPunksAssetTypeDto
import com.rarible.protocol.dto.Erc1155AssetTypeDto
import com.rarible.protocol.dto.Erc1155LazyAssetTypeDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.dto.Erc721LazyAssetTypeDto
import com.rarible.protocol.dto.GenerativeArtAssetTypeDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthAssetDto
import com.rarible.protocol.union.dto.EthAssetTypeDto
import com.rarible.protocol.union.dto.UnionCreatorDto
import com.rarible.protocol.union.dto.EthCryptoPunksAssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155AssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.EthGenerativeArtAssetTypeDto
import com.rarible.protocol.union.dto.EthOrderPayoutDto
import com.rarible.protocol.union.dto.UnionRoyaltyDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionActivitySortDto
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigDecimal
import java.math.MathContext

object EthConverter {

    private val FULL_100_PERCENTS_IN_BP = 10000.toBigDecimal()

    fun convertFromBp(value: Int): BigDecimal {
        return value.toBigDecimal().divide(FULL_100_PERCENTS_IN_BP, MathContext.DECIMAL128)
    }

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

    fun convertToPayout(source: PartDto, blockchain: BlockchainDto): EthOrderPayoutDto {
        return EthOrderPayoutDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigInteger()
        )
    }

    fun convertToRoyalty(source: PartDto, blockchain: BlockchainDto): UnionRoyaltyDto {
        return UnionRoyaltyDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = convertFromBp(source.value)
        )
    }

    fun convertToCreator(source: PartDto, blockchain: BlockchainDto): UnionCreatorDto {
        return UnionCreatorDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigDecimal()
        )
    }

    fun convert(source: AssetDto, blockchain: BlockchainDto): EthAssetDto {
        return EthAssetDto(
            type = convert(source.assetType, blockchain),
            value = source.value
        )
    }

    fun convert(source: AssetTypeDto, blockchain: BlockchainDto): EthAssetTypeDto {
        return when (source) {
            is com.rarible.protocol.dto.EthAssetTypeDto -> EthEthereumAssetTypeDto(
            )
            is Erc20AssetTypeDto -> EthErc20AssetTypeDto(
                contract = UnionAddressConverter.convert(source.contract, blockchain)
            )
            is Erc721AssetTypeDto -> EthErc721AssetTypeDto(
                contract = UnionAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId
            )
            is Erc1155AssetTypeDto -> EthErc1155AssetTypeDto(
                contract = UnionAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId
            )
            is Erc721LazyAssetTypeDto -> EthErc721LazyAssetTypeDto(
                contract = UnionAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId,
                uri = source.uri,
                creators = source.creators.map { convertToCreator(it, blockchain) },
                royalties = source.royalties.map { convertToRoyalty(it, blockchain) },
                signatures = source.signatures.map { convert(it) }
            )
            is Erc1155LazyAssetTypeDto -> EthErc1155LazyAssetTypeDto(
                contract = UnionAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId,
                uri = source.uri,
                supply = source.supply,
                creators = source.creators.map { convertToCreator(it, blockchain) },
                royalties = source.royalties.map { convertToRoyalty(it, blockchain) },
                signatures = source.signatures.map { convert(it) }
            )
            is CryptoPunksAssetTypeDto -> EthCryptoPunksAssetTypeDto(
                contract = UnionAddressConverter.convert(source.contract, blockchain),
                punkId = source.punkId
            )
            is GenerativeArtAssetTypeDto -> EthGenerativeArtAssetTypeDto(
                contract = UnionAddressConverter.convert(source.contract, blockchain)
            )
        }
    }
}