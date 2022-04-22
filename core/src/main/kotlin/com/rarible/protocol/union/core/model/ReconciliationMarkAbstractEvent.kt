package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "MARK", value = ReconciliationMarkEvent::class),
)
sealed class ReconciliationMarkAbstractEvent

data class ReconciliationMarkEvent(
    val entityId: String,
    val type: ReconciliationMarkType
) : ReconciliationMarkAbstractEvent()

enum class ReconciliationMarkType {
    ITEM,
    OWNERSHIP,
    COLLECTION
}

