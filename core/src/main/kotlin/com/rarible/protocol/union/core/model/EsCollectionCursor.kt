package com.rarible.protocol.union.core.model

import com.rarible.core.logging.Logger
import java.time.Instant

data class EsCollectionCursor(
    val date: Instant,
    val salt: Long,
) {
    override fun toString(): String {
        return "${date.toEpochMilli()}_${salt}"
    }

    companion object {
        private val logger by Logger()

        fun fromString(value: String): EsCollectionCursor? {
            return try {
                val split = value.split('_')
                EsCollectionCursor(
                    date = Instant.ofEpochMilli(split[0].toLong()),
                    salt = split[1].toLong()
                )
            } catch (e: RuntimeException) {
                logger.error("Failed to convert '$value' to CollectionCursor", e)
                null
            }
        }

        fun EsCollectionLite.fromCollectionLite(): EsCollectionCursor {
            return EsCollectionCursor(
                date = this.date,
                salt = this.salt,
            )
        }
    }
}
