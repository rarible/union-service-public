package com.rarible.protocol.union.core.converter.ethereum

import com.rarible.protocol.dto.AssetDto
import com.rarible.protocol.union.dto.EthAssetDto
import org.springframework.core.convert.converter.Converter

object EthAssetDtoConverter: Converter<AssetDto, EthAssetDto> {
    override fun convert(source: AssetDto): EthAssetDto {
        return EthAssetDto(
            assetType = EthAssetTypeDtoConverter.convert(source.assetType),
            value = source.value
        )
    }
}
