package com.rarible.protocol.union.enrichment.util

import com.rarible.core.common.nowMillis
import java.time.Duration
import java.time.Instant

fun spent(from: Instant): Long {
    return nowMillis().toEpochMilli() - from.toEpochMilli()
}

fun metaSpent(duration: Duration): String {
    // To make easier log filtration during investigations
    val spent = duration.toMillis()
    val speed = when {
        spent < 1000 -> "instant"
        spent < 5000 -> "fast"
        spent < 10000 -> "fine"
        spent < 20000 -> "slow"
        spent < 30000 -> "so-so"
        else -> "stuck"
    }
    return "${spent}ms - $speed"
}

fun metaSpent(from: Instant) = metaSpent(Duration.between(from, nowMillis()))
