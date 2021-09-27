package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowNftOwnershipDeleteEventDto
import com.rarible.protocol.dto.FlowNftOwnershipUpdateEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowOwnershipDeleteEventDto
import com.rarible.protocol.union.dto.FlowOwnershipUpdateEventDto
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import com.rarible.protocol.union.dto.parser.UnionOwnershipIdParser

object FlowUnionOwnershipEventConverter {

    fun convert(source: FlowOwnershipEventDto, blockchain: BlockchainDto): UnionOwnershipEventDto {
        val ownershipId = UnionOwnershipIdParser.parseShort(source.ownershipId, blockchain)
        return when (source) {
            is FlowNftOwnershipUpdateEventDto -> {
                FlowOwnershipUpdateEventDto(
                    eventId = source.eventId,
                    ownershipId = ownershipId,
                    ownership = FlowUnionOwnershipConverter.convert(source.ownership, blockchain)
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
