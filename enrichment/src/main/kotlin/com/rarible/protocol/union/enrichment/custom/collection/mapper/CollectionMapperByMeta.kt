package com.rarible.protocol.union.enrichment.custom.collection.mapper

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.custom.collection.provider.CustomCollectionProvider

class CollectionMapperByMeta(
    private val provider: CustomCollectionProvider,
    // attributeName -> acceptableValues
    private val attributes: Map<String, Set<String>>,
) : CollectionMapper {

    override suspend fun getCustomCollectionProviders(
        itemIds: Collection<ItemIdDto>,
        context: CollectionMapperContext
    ): Map<ItemIdDto, CustomCollectionProvider> {
        val meta = context.getItemsMeta(itemIds)
        return meta
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
