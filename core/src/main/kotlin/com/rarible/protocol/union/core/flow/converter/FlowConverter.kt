package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowAssetDto
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAssetNFTDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto

object FlowConverter {

    fun convert(source: FlowAssetDto, blockchain: BlockchainDto): AssetDto {
        return when (source) {
            is FlowAssetFungibleDto -> {
                AssetDto(
                    value = source.value.toBigInteger(), //TODO
                    type = FlowAssetTypeFtDto(
                        contract = UnionAddressConverter.convert(source.contract, blockchain)
                    )
                )
            }
            is FlowAssetNFTDto -> {
                AssetDto(
                    value = source.value.toBigInteger(), //TODO
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