package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.dto.FlowNftOwnershipDeleteEventDto
import com.rarible.protocol.dto.FlowNftOwnershipUpdateEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.serializer.flow.FlowOwnershipIdParser
import org.springframework.core.convert.converter.Converter
import java.math.BigInteger

object FlowUnionOwnershipEventDtoConverter : Converter<FlowOwnershipEventDto, UnionOwnershipEventDto> {

    override fun convert(source: FlowOwnershipEventDto): UnionOwnershipEventDto {
        val ownershipId = FlowOwnershipIdParser.parseShort(source.ownershipId)
        return when (source) {
            is FlowNftOwnershipUpdateEventDto -> {
                FlowOwnershipUpdateEventDto(
                    eventId = source.eventId,
                    ownershipId = ownershipId,
                    ownership = FlowOwnershipDto(
                        value = BigInteger.ONE,//TODO: Is it right?
                        createdAt = source.ownership.createdAt,
                        id = ownershipId,
                        contract = FlowContract(source.ownership.contract!!),
                        tokenId = source.ownership.tokenId.toBigInteger(), //TODO: Why is it string?
                        owner = listOf(FlowAddress(source.ownership.owner))
                    )
                )
            }
            is FlowNftOwnershipDeleteEventDto -> {
                FlowOwnershipDeleteEventDto(
                    eventId = source.eventId,
                    ownershipId = ownershipId
                )
            }
        }
    }
}
