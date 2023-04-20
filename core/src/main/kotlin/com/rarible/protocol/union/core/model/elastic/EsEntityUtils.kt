package com.rarible.protocol.union.core.model.elastic

import kotlin.random.Random.Default.nextLong

fun generateSalt(): Long {
    return nextLong(1, Long.MAX_VALUE)
}
