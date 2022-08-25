package com.rarible.protocol.union.enrichment.meta.item

// Ordered by priority
enum class ItemMetaPipeline(
    val pipeline: String
) {

    // Tasks triggered by Kafka events in listener, if there is no initial state of ItemMeta (not forced)
    EVENT("event"),

    // Tasks triggerred by API requests, if there is no initial state of ItemMeta (not forced)
    API("api"),

    // Tasks triggered by manual refresh (forced)
    REFRESH("refresh"),

    // Tasks triggerred by Retry job (not forced)
    RETRY("retry"),

    // For 'cold' start when we need to sync new blockchain's metadata
    SYNC("sync")
}