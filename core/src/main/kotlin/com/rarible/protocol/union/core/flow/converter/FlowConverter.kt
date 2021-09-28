package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowAssetDto
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAssetNFTDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionAssetDto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import com.rarible.protocol.union.dto.UnionAssetTypeDto

object FlowConverter {

    fun convert(source: FlowAssetDto, blockchain: BlockchainDto): UnionAssetDto {
        return when (source) {
            is FlowAssetFungibleDto -> {
                UnionAssetDto(
                    value = source.value.toBigInteger(), //TODO
                    type = FlowAssetTypeFtDto(
                        contract = UnionAddressConverter.convert(source.contract, blockchain)
                    )
                )
            }
            is FlowAssetNFTDto -> {
                UnionAssetDto(
                    value = source.value.toBigInteger(), //TODO
                    type = FlowAssetTypeNftDto(
                        contract = UnionAddressConverter.convert(source.contract, blockchain),
                        tokenId = source.tokenId
                    )
                )
            }
        }
    }

    fun convertToType(source: FlowAssetDto, blockchain: BlockchainDto): UnionAssetTypeDto {
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