package com.rarible.protocol.union.core.model.elasticsearch

enum class EsEntity {
    ACTIVITY,
    ORDER,
    COLLECTION,
    OWNERSHIP,
    ITEM;

    val entityName: String = name.lowercase()
}