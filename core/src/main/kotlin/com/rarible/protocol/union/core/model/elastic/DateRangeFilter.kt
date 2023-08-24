package com.rarible.protocol.union.core.model.elastic

import java.time.Instant

interface DateRangeFilter<T> {
    val from: Instant?
    val to: Instant?

    fun applyDateRange(range: DateRange): T
}

data class DateRange(
    val from: Instant,
    val to: Instant
)
