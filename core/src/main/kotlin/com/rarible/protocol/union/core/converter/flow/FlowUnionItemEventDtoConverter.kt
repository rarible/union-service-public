package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.union.dto.UnionItemEventDto
import org.springframework.core.convert.converter.Converter

object FlowUnionItemEventDtoConverter : Converter<FlowNftItemEventDto, UnionItemEventDto> {
    override fun convert(source: FlowNftItemEventDto): UnionItemEventDto {
        TODO()
    }
}

