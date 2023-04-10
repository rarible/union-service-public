package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import java.util.TreeSet

class CustomCollectionItemFetcherByList(
    private val customCollectionItemProvider: CustomCollectionItemProvider,
    items: List<ItemIdDto>
) : CustomCollectionItemFetcher {

    private val itemIds = TreeSet(items.map { it.fullId() })

    override suspend fun next(state: String?, batchSize: Int): CustomCollectionItemBatch {
        val after = state?.let { itemIds.tailSet(state, false) } ?: itemIds
        val batch = after.take(batchSize).map { IdParser.parseItemId(it) }
        val items = customCollectionItemProvider.fetch(batch)
        val result = items.sortedBy { it.id.fullId() }
        return CustomCollectionItemBatch(result.lastOrNull()?.id?.fullId(), result)
    }
}