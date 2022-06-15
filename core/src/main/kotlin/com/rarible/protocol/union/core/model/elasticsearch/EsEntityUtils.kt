package com.rarible.protocol.union.core.model.elasticsearch

import kotlin.random.Random.Default.nextLong

fun generateSalt(): Long {
    return nextLong(1, Long.MAX_VALUE)
}
