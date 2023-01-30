package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.CollectionIdDto
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "UPDATE", value = UnionItemUpdateEvent::class)
)

sealed class UnionCollectionEvent {

    abstract val collectionId: CollectionIdDto
    abstract val eventTimeMarks: UnionEventTimeMarks?
    abstract fun addTimeMark(name: String, date: Instant? = null): UnionCollectionEvent
}

data class UnionCollectionUpdateEvent(
    override val collectionId: CollectionIdDto,
    val collection: UnionCollection,
    override val eventTimeMarks: UnionEventTimeMarks?
) : UnionCollectionEvent() {

    constructor(
        collection: UnionCollection,
        eventTimeMarks: UnionEventTimeMarks?
    ) : this(collection.id, collection, eventTimeMarks)

    override fun addTimeMark(name: String, date: Instant?): UnionCollectionUpdateEvent {
        return this.copy(eventTimeMarks = this.eventTimeMarks?.add(name, date))
    }
}

