package com.rarible.protocol.union.enrichment.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "ITEM", value = ReconciliationItemMarkEvent::class),
    JsonSubTypes.Type(name = "OWNERSHIP", value = ReconciliationOwnershipMarkEvent::class)
)
sealed class ReconciliationMarkAbstractEvent

@Deprecated("Should be removed")
data class ReconciliationItemMarkEvent(
    val itemId: ItemIdDto
) : ReconciliationMarkAbstractEvent()

@Deprecated("Should be removed")
data class ReconciliationOwnershipMarkEvent(
    val ownershipId: OwnershipIdDto
) : ReconciliationMarkAbstractEvent()

data class ReconciliationMarkEvent(
    val entityId: String,
    val type: ReconciliationMarkType
) : ReconciliationMarkAbstractEvent()

enum class ReconciliationMarkType {
    ITEM,
    OWNERSHIP
}

