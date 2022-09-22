package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.protocol.union.dto.BlockchainDto
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.math.log

object TimePeriodContinuationHelper {

    private val logger = LoggerFactory.getLogger(javaClass)

    // From - the earliest time
    // To - the latest time
    // However, reindexing goes from the latest time to the earliest time, i.e. from "to" to "from"
    fun adjustContinuation(continuation: String?, from: Long?, to: Long?): String? {
        if (from == null && to == null) {
            return continuation
        }

        if (continuation.isNullOrEmpty()) {
            logger.info("Continuation is empty, stop reindexing")
            return continuation
        }

        val parts = ContinuationParts.parse(continuation)

        if (from != null && parts.timestamp < from) {
            logger.info("Continuation is below range end, stop reindexing")
            return null
        }

        if (to != null && parts.timestamp > to) {
            logger.info("Continuation is above range start, adjust to $to")
            parts.timestamp = to + 1 // +1 ms linger time
        }

        return parts.build()
    }


    data class ContinuationParts(
        val prefix: String,
        var timestamp: Long,
        val suffix: String
    ) {
        companion object {
            fun parse(continuation: String): ContinuationParts {
                val prefix = continuation.substringBefore(':')
                val suffix = continuation.substringAfter('_')
                val timestamp = continuation.substringAfter(':').substringBefore('_')
                return ContinuationParts(prefix, timestamp.toLong(), suffix)
            }
        }

        fun build(): String {
            return "$prefix:${timestamp}_$suffix"
        }
    }
}
