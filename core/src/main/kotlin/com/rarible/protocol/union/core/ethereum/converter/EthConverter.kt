package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.*
import com.rarible.protocol.dto.FlowAssetTypeDto
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.EthAssetTypeDto
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

object EthConverter {

    fun convert(address: Address) = address.prefixed()
    fun convert(word: Word) = word.prefixed()
    fun convert(binary: Binary) = binary.prefixed()

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
            assetType = convert(source.assetType, blockchain),
            value = source.value
        )
    }

    fun convert(source: AssetTypeDto, blockchain: EthBlockchainDto): EthAssetTypeDto {
        return when (source) {
            is com.rarible.protocol.dto.EthAssetTypeDto -> EthereumAssetDto(
                contract = EthAddressConverter.convert(Address.ZERO(), blockchain)
            )
            is Erc20AssetTypeDto -> ERC20AssetDto(
                contract = EthAddressConverter.convert(source.contract, blockchain)
            )
            is Erc721AssetTypeDto -> ERC721AssetDto(
                contract = EthAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId
            )
            is Erc1155AssetTypeDto -> ERC1155AssetDto(
                contract = EthAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId
            )
            is Erc721LazyAssetTypeDto -> ERC721LazyAssetDto(
                contract = EthAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId,
                uri = source.uri,
                creators = source.creators.map { convertToCreator(it, blockchain) },
                royalties = source.royalties.map { convertToRoyalty(it, blockchain) },
                signature = source.signatures.map { convert(it) }
            )
            is Erc1155LazyAssetTypeDto -> ERC1155LazyAssetDto(
                contract = EthAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId,
                uri = source.uri,
                supply = source.supply,
                creators = source.creators.map { convertToCreator(it, blockchain) },
                royalties = source.royalties.map { convertToRoyalty(it, blockchain) },
                signature = source.signatures.map { convert(it) }
            )
            is FlowAssetTypeDto -> TODO("Need remove from eth protocol-api")
        }
    }
}