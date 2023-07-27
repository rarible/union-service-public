package com.rarible.protocol.union.enrichment.custom.collection.mapper

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.custom.collection.provider.CustomCollectionProvider
import com.rarible.protocol.union.enrichment.model.ShortItem

/**
 * Mapper responsible for definition of custom collection for Item or Collection
 */
interface CollectionMapper {

    suspend fun getCustomCollectionProviders(
        itemIds: Collection<ItemIdDto>,
        // In some cases mapping might require additional data - meta, for example
        hint: Map<ItemIdDto, ShortItem>
    ): Map<ItemIdDto, CustomCollectionProvider>

    suspend fun getCustomCollectionProviders(
        collectionIds: Collection<CollectionIdDto>
    ): Map<CollectionIdDto, CustomCollectionProvider> = emptyMap()
}
