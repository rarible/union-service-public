package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.enrichment.event.ItemEvent
import com.rarible.protocol.union.enrichment.event.ItemEventDelete
import com.rarible.protocol.union.enrichment.event.ItemEventUpdate

object ItemEventToDtoConverter {

    fun convert(source: ItemEvent): ItemEventDto {
        return when (source) {
            is ItemEventUpdate -> ItemUpdateEventDto(
                eventId = source.id,
                itemId = source.item.id,
                item = source.item
            )
            is ItemEventDelete -> ItemDeleteEventDto(
                eventId = source.id,
                itemId = source.itemId
            )

        }
    }
}
