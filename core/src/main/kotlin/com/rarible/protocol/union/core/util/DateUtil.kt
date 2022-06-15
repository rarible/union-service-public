package com.rarible.protocol.union.core.util

import java.time.Instant
import java.time.temporal.ChronoUnit

fun Instant.truncatedToSeconds(): Instant {
    return truncatedTo(ChronoUnit.SECONDS)
}
