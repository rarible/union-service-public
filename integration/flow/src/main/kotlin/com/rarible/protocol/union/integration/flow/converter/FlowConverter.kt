package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowAssetDto
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAssetNFTDto
import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import com.rarible.protocol.union.dto.OrderPayoutDto

object FlowConverter {

    fun convertToPayout(source: PayInfoDto, blockchain: BlockchainDto): OrderPayoutDto {
        return OrderPayoutDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigInteger()
        )
    }

    fun convertToCreator(source: PayInfoDto, blockchain: BlockchainDto): CreatorDto {
        return CreatorDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value
        )
    }

    fun convert(source: FlowAssetDto, blockchain: BlockchainDto): AssetDto {
        return when (source) {
            is FlowAssetFungibleDto -> {
                AssetDto(
                    value = source.value,
                    type = FlowAssetTypeFtDto(
                        contract = UnionAddressConverter.convert(source.contract, blockchain)
                    )
                )
            }
            is FlowAssetNFTDto -> {
                AssetDto(
                    value = source.value,
                    type = FlowAssetTypeNftDto(
                        contract = UnionAddressConverter.convert(source.contract, blockchain),
                        tokenId = source.tokenId
                    )
                )
            }
        }
    }

    fun convertToType(source: FlowAssetDto, blockchain: BlockchainDto): AssetTypeDto {
        return when (source) {
            is FlowAssetFungibleDto -> {
                FlowAssetTypeFtDto(
                    contract = UnionAddressConverter.convert(source.contract, blockchain)
                )
            }
            is FlowAssetNFTDto -> {
                FlowAssetTypeNftDto(
                    contract = UnionAddressConverter.convert(source.contract, blockchain),
                    tokenId = source.tokenId
                )
            }
        }
    }

}