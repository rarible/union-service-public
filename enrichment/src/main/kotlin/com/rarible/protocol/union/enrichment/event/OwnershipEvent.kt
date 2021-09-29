package com.rarible.protocol.union.enrichment.event

import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import java.util.*

sealed class OwnershipEvent(
    val type: OwnershipEventType
) {
    val id: String = UUID.randomUUID().toString()
}

data class OwnershipEventUpdate(
    val ownership: OwnershipDto
) : OwnershipEvent(OwnershipEventType.UPDATE)


data class OwnershipEventDelete(
    val ownershipId: OwnershipIdDto
) : OwnershipEvent(OwnershipEventType.DELETE)

enum class OwnershipEventType {
    UPDATE, DELETE
}