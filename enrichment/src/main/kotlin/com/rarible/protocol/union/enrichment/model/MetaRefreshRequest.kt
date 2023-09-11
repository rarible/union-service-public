package com.rarible.protocol.union.enrichment.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("meta_refresh_request")
data class MetaRefreshRequest(
    @Id
    val id: String = ObjectId().toHexString(),
    val collectionId: String,
    val scheduledAt: Instant = Instant.now(),
    val scheduled: Boolean = false,
    val withSimpleHash: Boolean = false,
    val full: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val priority: Int = 0
)
