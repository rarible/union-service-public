package com.rarible.protocol.union.search.core.filter

import java.time.Instant

data class ActivitySort(
    val date: Instant,
    val blockNumber: Long?,
    val logIndex: Int?,
)