package com.rarible.protocol.union.enrichment.model

data class Marketplace(
    val id: String,
    val metaRefreshPriority: Int?,
    val collectionIds: Set<EnrichmentCollectionId>,
)
