package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAssetNFTDto
import com.rarible.protocol.union.dto.FlowAssetDto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import com.rarible.protocol.union.dto.FlowBlockchainDto

object FlowConverter {

    fun convert(source: com.rarible.protocol.dto.FlowAssetDto, blockchain: FlowBlockchainDto): FlowAssetDto {
        return when (source) {
            is FlowAssetFungibleDto -> {
                FlowAssetDto(
                    value = source.value.toBigInteger(), //TODO
                    type = FlowAssetTypeFtDto(
                        contract = FlowAddressConverter.convert(source.contract, blockchain)
                    )
                )
            }
            is FlowAssetNFTDto -> {
                FlowAssetDto(
                    value = source.value.toBigInteger(), //TODO
                    type = FlowAssetTypeNftDto(
                        contract = FlowAddressConverter.convert(source.contract, blockchain),
                        tokenId = source.tokenId
                    )
                )
            }
        }
    }

}