package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.event.OwnershipEvent
import com.rarible.protocol.union.enrichment.event.OwnershipEventDelete
import com.rarible.protocol.union.enrichment.event.OwnershipEventUpdate

object OwnershipEventToDtoConverter {

    fun convert(source: OwnershipEvent): OwnershipEventDto {
        return when (source) {
            is OwnershipEventUpdate -> OwnershipUpdateEventDto(
                eventId = source.id,
                ownershipId = source.ownership.id,
                ownership = source.ownership
            )
            is OwnershipEventDelete -> OwnershipDeleteEventDto(
                eventId = source.id,
                ownershipId = source.ownershipId
            )
        }
    }
}

