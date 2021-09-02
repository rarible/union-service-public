package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAssetNFTDto
import com.rarible.protocol.union.dto.FlowAssetDto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import org.springframework.core.convert.converter.Converter

object FlowAssetDtoConverter : Converter<com.rarible.protocol.dto.FlowAssetDto, FlowAssetDto> {

    override fun convert(source: com.rarible.protocol.dto.FlowAssetDto): FlowAssetDto {

        return when (source) {
            is FlowAssetFungibleDto -> {
                FlowAssetDto(
                    value = source.value.toBigInteger(), //TODO
                    assetType = FlowAssetTypeFtDto(
                        contract = FlowAddressConverter.convert(source.contract)
                    )
                )
            }
            is FlowAssetNFTDto -> {
                FlowAssetDto(
                    value = source.value.toBigInteger(), //TODO
                    assetType = FlowAssetTypeNftDto(
                        contract = FlowAddressConverter.convert(source.contract),
                        tokenId = source.tokenId
                    )
                )
            }
        }
    }
}
