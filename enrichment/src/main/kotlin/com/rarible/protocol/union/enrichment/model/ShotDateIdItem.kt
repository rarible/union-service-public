package com.rarible.protocol.union.enrichment.model

import java.time.Instant

data class ShotDateIdItem(
    val id: ShortItemId,
    val date: Instant
)