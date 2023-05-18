package com.rarible.protocol.union.enrichment.util

import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import java.math.BigInteger

data class TokenRange(
    val collection: EnrichmentCollectionId,
    val range: ClosedRange<BigInteger>
) {

    companion object {

        fun of(value: String): TokenRange {
            val collectionId = EnrichmentCollectionId.of(value.substringBeforeLast(":"))
            val rawRange = value.substringAfterLast(":").split("..")
            val range = rawRange[0].toBigInteger().rangeTo(rawRange[1].toBigInteger())
            return TokenRange(collectionId, range)
        }
    }

    fun batch(last: BigInteger?, size: Int): List<BigInteger> {
        val start = last?.add(BigInteger.ONE) ?: range.start
        val end = range.endInclusive.min(start.add((size - 1).toBigInteger()))

        var current = start
        val result = ArrayList<BigInteger>(size)
        while (current <= end) {
            result.add(current)
            current = current.inc()
        }
        return result
    }

}