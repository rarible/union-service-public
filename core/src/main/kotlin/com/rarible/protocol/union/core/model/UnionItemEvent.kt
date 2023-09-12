package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.ItemIdDto
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "UPDATE", value = UnionItemUpdateEvent::class),
    JsonSubTypes.Type(name = "DELETE", value = UnionItemDeleteEvent::class),
    JsonSubTypes.Type(name = "CHANGE", value = UnionItemChangeEvent::class),
)
sealed class UnionItemEvent {

    abstract val itemId: ItemIdDto
    abstract val eventTimeMarks: UnionEventTimeMarks?
    abstract fun addTimeMark(name: String, date: Instant? = null): UnionItemEvent
}

data class UnionItemUpdateEvent(
    override val itemId: ItemIdDto,
    val item: UnionItem,
    override val eventTimeMarks: UnionEventTimeMarks?
) : UnionItemEvent() {

    constructor(item: UnionItem, eventTimeMarks: UnionEventTimeMarks?) : this(item.id, item, eventTimeMarks)

    override fun addTimeMark(name: String, date: Instant?): UnionItemUpdateEvent {
        return this.copy(eventTimeMarks = this.eventTimeMarks?.add(name, date))
    }
}

data class UnionItemDeleteEvent(
    override val itemId: ItemIdDto,
    override val eventTimeMarks: UnionEventTimeMarks?
) : UnionItemEvent() {

    override fun addTimeMark(name: String, date: Instant?): UnionItemDeleteEvent {
        return this.copy(eventTimeMarks = this.eventTimeMarks?.add(name, date))
    }
}

data class UnionItemChangeEvent(
    override val itemId: ItemIdDto,
    override val eventTimeMarks: UnionEventTimeMarks?
) : UnionItemEvent() {

    override fun addTimeMark(name: String, date: Instant?): UnionItemChangeEvent {
        return this.copy(eventTimeMarks = this.eventTimeMarks?.add(name, date))
    }
}
