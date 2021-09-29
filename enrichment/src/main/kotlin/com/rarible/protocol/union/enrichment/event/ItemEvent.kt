package com.rarible.protocol.union.enrichment.event

import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import java.util.*

sealed class ItemEvent(
    val type: ItemEventType
) {
    val id: String = UUID.randomUUID().toString()
}

data class ItemEventUpdate(
    val item: ItemDto
) : ItemEvent(ItemEventType.UPDATE)


data class ItemEventDelete(
    val itemId: ItemIdDto
) : ItemEvent(ItemEventType.DELETE)

enum class ItemEventType {
    UPDATE, DELETE
}
