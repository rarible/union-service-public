package com.rarible.protocol.union.enrichment.custom.collection.mapper

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionItemProvider
import com.rarible.protocol.union.enrichment.custom.collection.provider.CustomCollectionProvider
import com.rarible.protocol.union.enrichment.model.ShortItem

class CollectionMapperByMeta(
    private val provider: CustomCollectionProvider,
    // attributeName -> acceptableValues
    private val attributes: Map<String, Set<String>>,
    private val customCollectionItemProvider: CustomCollectionItemProvider
) : CollectionMapper {

    override suspend fun getCustomCollectionProviders(
        itemIds: Collection<ItemIdDto>,
        hint: Map<ItemIdDto, ShortItem>
    ): Map<ItemIdDto, CustomCollectionProvider> {
        val missing = itemIds.filter { !hint.containsKey(it) }

        val fetched = customCollectionItemProvider.getOrFetchMeta(missing)

        val fromHint = hint.filter { it.value.metaEntry?.data != null }
            .mapValues { it.value.metaEntry!!.data!! }

        return (fetched + fromHint)
            .filter { matches(it.value) }
            .mapValues { provider }
    }

    private fun matches(meta: UnionMeta): Boolean {
        meta.attributes.forEach { attr ->
            attributes[attr.key]?.let {
                if (it.contains(attr.value)) return true
            }
        }
        return false
    }
}