package com.rarible.protocol.union.worker.task.search.activity

import org.slf4j.LoggerFactory

@Deprecated("Replace with SyncActivityJob")
object TimePeriodContinuationHelper {

    private val logger = LoggerFactory.getLogger(javaClass)

    // From - the earliest time
    // To - the latest time
    // However, reindexing goes from the latest time to the earliest time, i.e. from "to" to "from"
    fun adjustContinuation(continuation: String?, from: Long?, to: Long?, hasPrefix: Boolean = true): String? {
        if (from == null && to == null) {
            return continuation
        }

        if (continuation.isNullOrEmpty()) {
            logger.info("Continuation is empty, stop reindexing")
            return continuation
        }

        val parts = ContinuationParts.parse(continuation, hasPrefix)

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
        val prefix: String?,
        var timestamp: Long,
        val suffix: String,
        val hasPrefix : Boolean = true,
    ) {
        companion object {
            fun parse(continuation: String, hasPrefix: Boolean = true): ContinuationParts {
                val prefix = if (hasPrefix) continuation.substringBefore(':') else null
                val suffix = continuation.substringAfter('_')
                val timestamp = if (hasPrefix) {
                    continuation.substringAfter(':').substringBefore('_')
                } else {
                    continuation.substringBefore('_')
                }

                return ContinuationParts(prefix, timestamp.toLong(), suffix, hasPrefix)
            }
        }

        fun build(): String {
            val prefixString = if (hasPrefix) "$prefix:" else ""
            return "${prefixString}${timestamp}_$suffix"
        }
    }
}
