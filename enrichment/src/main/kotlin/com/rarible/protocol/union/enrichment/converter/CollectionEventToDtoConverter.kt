package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.enrichment.event.CollectionEvent
import com.rarible.protocol.union.enrichment.event.CollectionEventUpdate

object CollectionEventToDtoConverter {

    fun convert(source: CollectionEvent): CollectionEventDto {
        return when (source) {
            is CollectionEventUpdate -> CollectionUpdateEventDto(
                collection = source.collection,
                collectionId = source.collection.id,
                eventId = source.id
            )
        }
    }
}
