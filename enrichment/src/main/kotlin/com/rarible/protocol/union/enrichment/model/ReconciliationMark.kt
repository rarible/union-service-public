package com.rarible.protocol.union.enrichment.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("reconciliation_mark")
data class ReconciliationMark(
    @Id
    val id: String,
    val type: ReconciliationMarkType,
    val lastUpdatedAt: Instant,
    val retries: Int = 0
)
