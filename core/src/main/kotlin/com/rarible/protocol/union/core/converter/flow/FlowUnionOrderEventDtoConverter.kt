package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.union.dto.UnionOrderEventDto
import org.springframework.core.convert.converter.Converter

object FlowUnionOrderEventDtoConverter : Converter<FlowOrderEventDto, UnionOrderEventDto> {
    override fun convert(source: FlowOrderEventDto): UnionOrderEventDto {
        TODO()
    }
}
