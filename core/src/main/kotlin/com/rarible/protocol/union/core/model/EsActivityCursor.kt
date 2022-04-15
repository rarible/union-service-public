package com.rarible.protocol.union.core.model

import com.rarible.core.logging.Logger
import java.time.Instant

data class EsActivityCursor(
    val date: Instant,
    val blockNumber: Long?,
    val logIndex: Int?,
    val salt: Long,
) {
    override fun toString(): String {
        return "${date.toEpochMilli()}_${blockNumber}_${logIndex}_${salt}"
    }

    companion object {
        private val logger by Logger()

        fun fromString(value: String): EsActivityCursor? {
            return try {
                val split = value.split('_')
                EsActivityCursor(
                    date = Instant.ofEpochMilli(split[0].toLong()),
                    blockNumber = split[1].toLongOrNull(),
                    logIndex = split[2].toIntOrNull(),
                    salt = split[3].toLong()
                )
            } catch (e: RuntimeException) {
                logger.error("Failed to convert '$value' to ActivityCursor", e)
                null
            }
        }

        fun EsActivity.fromActivity(): EsActivityCursor {
            return EsActivityCursor(
                date = this.date,
                blockNumber = this.blockNumber,
                logIndex = this.logIndex,
                salt = this.salt,
            )
        }
    }
}
