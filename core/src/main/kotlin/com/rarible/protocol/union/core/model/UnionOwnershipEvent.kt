package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.OwnershipIdDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "UPDATE", value = UnionOwnershipUpdateEvent::class),
    JsonSubTypes.Type(name = "DELETE", value = UnionOwnershipDeleteEvent::class)
)
sealed class UnionOwnershipEvent {

    abstract val ownershipId: OwnershipIdDto
}

data class UnionOwnershipUpdateEvent(
    override val ownershipId: OwnershipIdDto,
    val ownership: UnionOwnership
) : UnionOwnershipEvent() {

    constructor(ownership: UnionOwnership) : this(ownership.id, ownership)

}

data class UnionOwnershipDeleteEvent(
    override val ownershipId: OwnershipIdDto
) : UnionOwnershipEvent()