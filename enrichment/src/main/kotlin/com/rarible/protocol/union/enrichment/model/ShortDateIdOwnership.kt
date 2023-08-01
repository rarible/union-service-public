package com.rarible.protocol.union.enrichment.model

import java.time.Instant

data class ShortDateIdOwnership(
    val id: ShortOwnershipId,
    val lastUpdatedAt: Instant
)