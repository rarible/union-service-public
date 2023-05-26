package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.util.TokenRange
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class CustomCollectionItemFetcherByRange(
    private val customCollectionItemProvider: CustomCollectionItemProvider,
    private val ranges: List<TokenRange>
) : CustomCollectionItemFetcher {

    override suspend fun next(state: String?, batchSize: Int): CustomCollectionItemBatch {
        val current = state?.let { State(state) }
        val currentRange = AtomicInteger(current?.range ?: 0)
        val lastTokenId = AtomicReference(current?.state)
        while (currentRange.get() < ranges.size) {
            val range = ranges[currentRange.get()]
            val collection = range.collection
            val next = range.batch(lastTokenId.get(), batchSize)
            if (next.isNotEmpty()) {
                val itemIds = next.map { ItemIdDto(collection.blockchain, "${collection.collectionId}:${it}") }
                val items = customCollectionItemProvider.fetch(itemIds)
                return CustomCollectionItemBatch(State(currentRange.get(), next.last()).toString(), items)
            }
            currentRange.incrementAndGet()
            lastTokenId.set(null)
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


