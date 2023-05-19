package com.rarible.protocol.union.enrichment.custom.collection.mapper

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.model.ShortItem

interface CollectionMapper {

    suspend fun getCustomCollections(
        itemIds: Collection<ItemIdDto>,
        hint: Map<ItemIdDto, ShortItem>
    ): Map<ItemIdDto, CollectionIdDto>

    suspend fun getCustomCollections(
        collectionIds: Collection<CollectionIdDto>
    ): Map<CollectionIdDto, CollectionIdDto> = emptyMap()
}