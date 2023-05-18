package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.util.TokenRange
import java.math.BigInteger

class CustomCollectionItemFetcherByRange(
    private val customCollectionItemProvider: CustomCollectionItemProvider,
    private val ranges: List<TokenRange>
) : CustomCollectionItemFetcher {

    override suspend fun next(state: String?, batchSize: Int): CustomCollectionItemBatch {
        val current = state?.let { State(state) }
        var currentRange = current?.range ?: 0
        var lastTokenId = current?.state
        while (currentRange < ranges.size) {
            val range = ranges[currentRange]
            val collection = range.collection
            val next = range.batch(lastTokenId, batchSize)
            if (next.isNotEmpty()) {
                val itemIds = next.map { ItemIdDto(collection.blockchain, "${collection.collectionId}:${it}") }
                val items = customCollectionItemProvider.fetch(itemIds)
                return CustomCollectionItemBatch(State(currentRange, next.last()).toString(), items)
            }
            currentRange++
            lastTokenId = null
        }
        return CustomCollectionItemBatch(null, emptyList())
    }

    private data class State(
        val range: Int,
        val state: BigInteger
    ) {

        constructor(state: String) : this(
            state.substringBefore("-").toInt(),
            state.substringAfter("-").toBigInteger()
        )

        override fun toString(): String {
            return "${range}-${state}"
        }
    }
}


