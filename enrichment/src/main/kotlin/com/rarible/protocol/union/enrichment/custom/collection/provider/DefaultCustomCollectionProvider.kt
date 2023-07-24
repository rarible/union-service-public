package com.rarible.protocol.union.enrichment.custom.collection.provider

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.ShortItem

class DefaultCustomCollectionProvider(
    private val customCollectionId: CollectionIdDto
) : CustomCollectionProvider {

    override suspend fun getCustomCollection(
        itemId: ItemIdDto,
        item: ShortItem?
    ): CollectionIdDto {
        return customCollectionId
    }

    override suspend fun getCustomCollection(
        collectionId: CollectionIdDto,
        collection: EnrichmentCollection?
    ): CollectionIdDto {
        return customCollectionId
    }
}
