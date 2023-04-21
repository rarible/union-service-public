package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.OwnershipIdDto
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "UPDATE", value = UnionOwnershipUpdateEvent::class),
    JsonSubTypes.Type(name = "DELETE", value = UnionOwnershipDeleteEvent::class),
    JsonSubTypes.Type(name = "CHANGE", value = UnionOwnershipChangeEvent::class)
)
sealed class UnionOwnershipEvent {

    abstract val ownershipId: OwnershipIdDto
    abstract val eventTimeMarks: UnionEventTimeMarks?
    abstract fun addTimeMark(name: String, date: Instant? = null): UnionOwnershipEvent
}

data class UnionOwnershipUpdateEvent(
    override val ownershipId: OwnershipIdDto,
    val ownership: UnionOwnership,
    override val eventTimeMarks: UnionEventTimeMarks?
) : UnionOwnershipEvent() {

    constructor(
        ownership: UnionOwnership,
        eventTimeMarks: UnionEventTimeMarks?
    ) : this(ownership.id, ownership, eventTimeMarks)

    override fun addTimeMark(name: String, date: Instant?): UnionOwnershipUpdateEvent {
        return this.copy(eventTimeMarks = this.eventTimeMarks?.add(name, date))
    }
}

data class UnionOwnershipDeleteEvent(
    override val ownershipId: OwnershipIdDto,
    override val eventTimeMarks: UnionEventTimeMarks?
) : UnionOwnershipEvent() {

    override fun addTimeMark(name: String, date: Instant?): UnionOwnershipDeleteEvent {
        return this.copy(eventTimeMarks = this.eventTimeMarks?.add(name, date))
    }

}

data class UnionOwnershipChangeEvent(
    override val ownershipId: OwnershipIdDto,
    override val eventTimeMarks: UnionEventTimeMarks?
) : UnionOwnershipEvent() {

    override fun addTimeMark(name: String, date: Instant?): UnionOwnershipChangeEvent {
        return this.copy(eventTimeMarks = this.eventTimeMarks?.add(name, date))
    }

}