package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.dto.FlowAssetDto
import org.springframework.core.convert.converter.Converter

object FlowAssetDtoConverter : Converter<FlowAssetDto, com.rarible.protocol.union.dto.FlowAssetDto> {
    override fun convert(source: FlowAssetDto): com.rarible.protocol.union.dto.FlowAssetDto {
        TODO("Not yet implemented")
    }
}
