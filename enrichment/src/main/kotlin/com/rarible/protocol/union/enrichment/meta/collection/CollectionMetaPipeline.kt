package com.rarible.protocol.union.enrichment.meta.collection

// Ordered by priority
enum class CollectionMetaPipeline(
    val pipeline: String
) {

    // Tasks triggered by Kafka events in listener, if there is no initial state of CollectionMeta (not forced)
    EVENT("event"),

    // Tasks triggerred by API requests, if there is no initial state of CollectionMeta (not forced)
    API("api"),

    // Tasks triggered by manual refresh (forced)
    REFRESH("refresh"),

    // Tasks triggerred by Retry job (not forced)
    RETRY("retry"),

    // For 'cold' start when we need to sync new blockchain's metadata
    SYNC("sync")
}