package com.rarible.protocol.union.core.model.elastic

enum class EsEntity {
    ACTIVITY,
    ORDER,
    COLLECTION,
    OWNERSHIP,
    ITEM,
    TRAIT;

    val entityName: String = name.lowercase()
}
