package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAssetNFTDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowAssetDto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto

object FlowConverter {

    fun convert(source: com.rarible.protocol.dto.FlowAssetDto, blockchain: BlockchainDto): FlowAssetDto {
        return when (source) {
            is FlowAssetFungibleDto -> {
                FlowAssetDto(
                    value = source.value.toBigInteger(), //TODO
                    type = FlowAssetTypeFtDto(
                        contract = UnionAddressConverter.convert(source.contract, blockchain)
                    )
                )
            }
            is FlowAssetNFTDto -> {
                FlowAssetDto(
                    value = source.value.toBigInteger(), //TODO
                    type = FlowAssetTypeNftDto(
                        contract = UnionAddressConverter.convert(source.contract, blockchain),
                        tokenId = source.tokenId
                    )
                )
            }
        }
    }

}