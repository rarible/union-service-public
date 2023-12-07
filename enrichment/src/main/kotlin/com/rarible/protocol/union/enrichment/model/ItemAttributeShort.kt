package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.core.util.trimToLength

data class ItemAttributeShort(
    val key: String,
    val value: String,
) {
    override fun toString(): String {
        return "key=${trimToLength(key, 32)},value=${trimToLength(value, 32)}"
    }
}
