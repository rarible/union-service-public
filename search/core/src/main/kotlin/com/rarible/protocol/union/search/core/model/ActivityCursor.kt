package com.rarible.protocol.union.search.core.model

import java.time.Instant

data class ActivityCursor(
    val date: Instant,
    val blockNumber: Long?,
    val logIndex: Int?,
    val salt: Long,
) {
    override fun toString(): String {
        return "${date.toEpochMilli()}_${blockNumber}_${logIndex}_${salt}"
    }

    companion object {
        fun fromString(value: String): ActivityCursor {
            val splitted = value.split('_')
            return ActivityCursor(
                date = Instant.ofEpochMilli(splitted[0].toLong()),
                blockNumber = splitted[1].toLongOrNull(),
                logIndex = splitted[2].toIntOrNull(),
                salt = splitted[3].toLong()
            )
        }
    }
}
