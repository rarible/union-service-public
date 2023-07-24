package com.rarible.protocol.union.enrichment.custom.collection.mapper

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.custom.collection.provider.CustomCollectionProvider
import com.rarible.protocol.union.enrichment.model.ShortItem

class CollectionMapperDirect(
    private val provider: CustomCollectionProvider
) : CollectionMapper {

    override suspend fun getCustomCollectionProviders(
        itemIds: Collection<ItemIdDto>,
        hint: Map<ItemIdDto, ShortItem>
    ): Map<ItemIdDto, CustomCollectionProvider> {
        return itemIds.associateWith { provider }
    }

    override suspend fun getCustomCollectionProviders(
        collectionIds: Collection<CollectionIdDto>
    ): Map<CollectionIdDto, CustomCollectionProvider> {
        return collectionIds.associateWith { provider }
    }
}