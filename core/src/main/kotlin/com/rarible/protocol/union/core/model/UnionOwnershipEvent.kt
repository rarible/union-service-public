package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.OwnershipIdDto

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