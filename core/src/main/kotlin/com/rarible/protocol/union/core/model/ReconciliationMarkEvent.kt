package com.rarible.protocol.union.core.model

data class ReconciliationMarkEvent(
    val entityId: String,
    val type: ReconciliationMarkType
)

enum class ReconciliationMarkType {
    ITEM,
    OWNERSHIP,
    COLLECTION
}
