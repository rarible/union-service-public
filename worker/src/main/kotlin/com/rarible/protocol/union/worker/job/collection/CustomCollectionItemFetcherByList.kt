package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import java.util.TreeSet

class CustomCollectionItemFetcherByList(
    private val customCollectionItemProvider: CustomCollectionItemProvider,
    items: List<ItemIdDto>
) : CustomCollectionItemFetcher {

    private val itemIds = run {
        val result = TreeSet(ItemIdDto.Comparators.LEXICOGRAPHICAL)
        result.addAll(items)
        result
    }

    override suspend fun next(state: String?, batchSize: Int): CustomCollectionItemBatch {
        val after = state?.let { itemIds.tailSet(IdParser.parseItemId(it), false) } ?: itemIds
        val batch = after.take(batchSize)
        val items = customCollectionItemProvider.fetch(batch)
        return CustomCollectionItemBatch(batch.lastOrNull()?.fullId(), items)
    }
}