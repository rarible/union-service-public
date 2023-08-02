package com.rarible.protocol.union.enrichment.model

import java.time.Instant

data class ShortDateIdItem(
    val id: ShortItemId,
    val lastUpdatedAt: Instant
)

