package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import org.springframework.core.convert.converter.Converter

object FlowUnionOwnershipEventDtoConverter : Converter<FlowOwnershipEventDto, UnionOwnershipEventDto> {
    override fun convert(source: FlowOwnershipEventDto): UnionOwnershipEventDto {
        TODO()
    }
}
