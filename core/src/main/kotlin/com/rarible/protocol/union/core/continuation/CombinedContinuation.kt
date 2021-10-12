package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.dto.continuation.Continuation

data class CombinedContinuation(
    val continuations: Map<String, String>
) {

    override fun toString(): String {
        return continuations.entries.joinToString(separator = ";") {
            "${it.key}:${it.value}"
        }
    }

    companion object {
        fun parse(str: String?): CombinedContinuation {
            if (str == null || str.isEmpty()) {
                return CombinedContinuation(emptyMap())
            }
            val continuations = str.split(";")
            val mapped = continuations.mapNotNull {
                Continuation.splitBy(it, ":")
            }.associateBy({ it.first }, { it.second })

            return CombinedContinuation(mapped)
        }
    }
}