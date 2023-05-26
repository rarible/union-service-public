package com.rarible.protocol.union.enrichment.custom.collection.mapper

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.model.ShortItem

class CollectionMapperDirect(
    private val collectionId: CollectionIdDto
) : CollectionMapper {

    override suspend fun getCustomCollections(
        itemIds: Collection<ItemIdDto>,
        hint: Map<ItemIdDto, ShortItem>
    ): Map<ItemIdDto, CollectionIdDto> {
        return itemIds.associateWith { collectionId }
    }

    override suspend fun getCustomCollections(
        collectionIds: Collection<CollectionIdDto>
    ): Map<CollectionIdDto, CollectionIdDto> {
        return collectionIds.associateWith { collectionId }
    }
}