package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowNftOwnershipDeleteEventDto
import com.rarible.protocol.dto.FlowNftOwnershipUpdateEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionOwnershipDeleteEventDto
import com.rarible.protocol.union.dto.UnionOwnershipUpdateEventDto
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import com.rarible.protocol.union.dto.parser.UnionOwnershipIdParser

object FlowUnionOwnershipEventConverter {

    fun convert(source: FlowOwnershipEventDto, blockchain: BlockchainDto): UnionOwnershipEventDto {
        val ownershipId = UnionOwnershipIdParser.parseShort(source.ownershipId, blockchain)
        return when (source) {
            is FlowNftOwnershipUpdateEventDto -> {
                UnionOwnershipUpdateEventDto(
                    eventId = source.eventId,
                    ownershipId = ownershipId,
                    ownership = FlowUnionOwnershipConverter.convert(source.ownership, blockchain)
                )
            }
            is FlowNftOwnershipDeleteEventDto -> {
                UnionOwnershipDeleteEventDto(
                    eventId = source.eventId,
                    ownershipId = ownershipId
                )
            }
        }
    }
}
