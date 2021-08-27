package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*
import org.springframework.core.convert.converter.Converter
import java.math.BigInteger
import java.time.Instant

object FlowUnionOwnershipEventDtoConverter : Converter<FlowOwnershipEventDto, UnionOwnershipEventDto> {
    override fun convert(source: FlowOwnershipEventDto): UnionOwnershipEventDto {
        return when (source) {
            is FlowNftOwnershipUpdateEventDto -> {
                UnionOwnershipUpdateEventDto(
                    eventId = source.eventId,
                    ownershipId = OwnershipId(source.ownershipId),
                    ownership = FlowOwnershipDto(
                        value = BigInteger.ONE,//TODO: Is it right?
                        createdAt = source.ownership.date ?: Instant.now(), //TODO: Must not be null
                        id  = FlowOwnershipId(source.ownership.id ?: ""), //TODO: Must not be null
                        contract = FlowContract(source.ownership.token),
                        tokenId = source.ownership.tokenId.toBigInteger(), //TODO: Why is it string?
                        owner = listOf(FlowAddress(source.ownership.owner))
                    )
                )
            }
            is FlowNftOwnershipDeleteEventDto -> {
                UnionOwnershipDeleteEventDto(
                    eventId = source.eventId,
                    ownershipId = OwnershipId(source.ownershipId)
                )
            }
        }
    }
}
