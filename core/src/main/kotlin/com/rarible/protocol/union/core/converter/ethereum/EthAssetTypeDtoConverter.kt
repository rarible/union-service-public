package com.rarible.protocol.union.core.converter.ethereum

import com.rarible.protocol.dto.*
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.FlowAssetTypeDto
import com.rarible.protocol.union.dto.*
import org.springframework.core.convert.converter.Converter
import scalether.domain.Address
import com.rarible.protocol.union.dto.EthAssetTypeDto as UnionEthAssetTypeDto

object EthAssetTypeDtoConverter: Converter<AssetTypeDto, UnionEthAssetTypeDto> {
    override fun convert(source: AssetTypeDto): UnionEthAssetTypeDto {
        return when (source) {
            is EthAssetTypeDto -> EthereumAssetDto(EthAddressConverter.convert(Address.ZERO()))
            is Erc20AssetTypeDto -> ERC20AssetDto(EthAddressConverter.convert(source.contract))
            is Erc721AssetTypeDto -> ERC721AssetDto(EthAddressConverter.convert(source.contract), source.tokenId)
            is Erc1155AssetTypeDto -> ERC1155AssetDto(EthAddressConverter.convert(source.contract), source.tokenId)
            is Erc721LazyAssetTypeDto -> ERC721LazyAssetDto(
                contract = EthAddressConverter.convert(source.contract),
                tokenId  = source.tokenId,
                uri = source.uri,
                creators = source.creators.map { EthCreatorDtoConverter.convert(it) },
                royalties = source.royalties.map { EthRoyaltyDtoConverter.convert(it) },
                signature = source.signatures.map { EthTypesConverter.convert(it) }
            )
            is Erc1155LazyAssetTypeDto -> ERC1155LazyAssetDto(
                contract = EthAddressConverter.convert(source.contract),
                tokenId  = source.tokenId,
                uri = source.uri,
                supply = source.supply,
                creators = source.creators.map { EthCreatorDtoConverter.convert(it) },
                royalties = source.royalties.map { EthRoyaltyDtoConverter.convert(it) },
                signature = source.signatures.map { EthTypesConverter.convert(it) }
            )
            is FlowAssetTypeDto -> TODO("Need remove from eth protocol-api")
        }
    }
}
