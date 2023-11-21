package com.rarible.protocol.union.enrichment.custom.collection.mapper

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.custom.collection.provider.CustomCollectionProvider

class CollectionMapperDirect(
    private val provider: CustomCollectionProvider
) : CollectionMapper {

    override suspend fun getCustomCollectionProviders(
        itemIds: Collection<ItemIdDto>,
        context: CollectionMapperContext
    ): Map<ItemIdDto, CustomCollectionProvider> {
        return itemIds.associateWith { provider }
    }

    override suspend fun getCustomCollectionProviders(
        collectionIds: Collection<CollectionIdDto>
    ): Map<CollectionIdDto, CustomCollectionProvider> {
        return collectionIds.associateWith { provider }
    }
}
