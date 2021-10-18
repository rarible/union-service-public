package com.rarible.protocol.union.enrichment.event

import com.rarible.protocol.union.dto.CollectionDto
import java.util.*

sealed class CollectionEvent(
    val type: CollectionEventType
) {
    val id: String = UUID.randomUUID().toString()
}

data class CollectionEventUpdate(
    val collection: CollectionDto
) : CollectionEvent(CollectionEventType.UPDATE)

enum class CollectionEventType {
    UPDATE
}
