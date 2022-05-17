package com.rarible.protocol.union.core.model

import com.rarible.core.logging.Logger
import java.time.Instant

data class EsItemCursor(
    val date: Instant,
    val itemId: String?,
) {
    override fun toString(): String {
        return "${date.toEpochMilli()}_${itemId}"
    }

    companion object {
        private val logger by Logger()

        fun fromString(value: String): EsItemCursor? {
            return try {
                val split = value.split('_')
                EsItemCursor(
                    date = Instant.ofEpochMilli(split[0].toLong()),
                    itemId = split[1],
                )
            } catch (e: RuntimeException) {
                logger.error("Failed to convert '$value' to ItemCursor", e)
                null
            }
        }

        fun EsItem.fromItem(): EsItemCursor {
            return EsItemCursor(
                date = lastUpdatedAt,
                itemId = itemId,
            )
        }
    }
}
