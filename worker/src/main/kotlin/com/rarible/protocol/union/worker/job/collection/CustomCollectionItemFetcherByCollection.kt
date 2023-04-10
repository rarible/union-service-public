package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.parser.IdParser
import java.util.TreeSet

class CustomCollectionItemFetcherByCollection(
    private val customCollectionItemProvider: CustomCollectionItemProvider,
    collections: List<CollectionIdDto>
) : CustomCollectionItemFetcher {

    private val collectionIds = TreeSet(collections.map { it.fullId() })

    override suspend fun next(state: String?, batchSize: Int): CustomCollectionItemBatch {
        // We need to take collection from current state (which is itemId), but if that's the first
        // request, we should use first collection from the list
        val continuation = DateIdContinuation.parse(state)
        val itemId = continuation?.id?.let { IdParser.parseItemId(it) }
        val collectionId = itemId?.let { customCollectionItemProvider.getItemCollectionId(itemId) }
            ?: IdParser.parseCollectionId(collectionIds.first())

        var items = customCollectionItemProvider.fetch(collectionId, state, batchSize)

        // If there are no items left for this collection, switching to next from the config
        var currentCollectionId = collectionId.fullId()
        while (items.isEmpty()) {
            val nextCollectionId = collectionIds.tailSet(currentCollectionId, false).firstOrNull()
                ?: return CustomCollectionItemBatch.empty()
            items = customCollectionItemProvider.fetch(IdParser.parseCollectionId(nextCollectionId), null, batchSize)
            currentCollectionId = nextCollectionId
        }

        val last = items.last()
        val cursor = DateIdContinuation(last.lastUpdatedAt, last.id.fullId())
        return CustomCollectionItemBatch(cursor.toString(), items)
    }
}
