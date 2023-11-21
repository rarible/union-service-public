package com.rarible.protocol.union.enrichment.custom.collection.mapper

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionItemProvider
import com.rarible.protocol.union.enrichment.model.ShortItem
import java.util.concurrent.ConcurrentHashMap

class CollectionMapperContext(
    itemHint: Map<ItemIdDto, ShortItem>,
    private val provider: CustomCollectionItemProvider,
) {

    private val itemHint = ConcurrentHashMap(itemHint)
    private val itemMetaHint = ConcurrentHashMap(
        itemHint.values.filter { it.metaEntry?.data != null }
            .associateBy({ it.id.toDto() }, { it.metaEntry!!.data!! })
    )

    suspend fun getItemsMeta(itemIds: Collection<ItemIdDto>): Map<ItemIdDto, UnionMeta> {
        val cached = HashMap<ItemIdDto, UnionMeta>()
        val missingIds = itemIds.filter { itemId ->
            val fromCache = itemMetaHint[itemId]
            fromCache?.let { cached[itemId] = it }
            fromCache == null
        }

        val fetched = provider.getOrFetchMeta(missingIds)
        itemMetaHint.putAll(fetched)

        return cached + fetched
    }

    fun getItemHint(): Map<ItemIdDto, ShortItem> = itemHint
}
