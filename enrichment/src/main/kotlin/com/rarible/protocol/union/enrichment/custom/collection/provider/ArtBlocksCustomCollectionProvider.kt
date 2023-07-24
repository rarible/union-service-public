package com.rarible.protocol.union.enrichment.custom.collection.provider

import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionItemProvider
import com.rarible.protocol.union.enrichment.model.ShortItem
import org.springframework.stereotype.Component

@Component
class ArtBlocksCustomCollectionProvider(
    private val artificialCollectionService: ArtificialCollectionService,
    private val customCollectionItemProvider: CustomCollectionItemProvider
) : CustomCollectionProvider {

    override suspend fun getCustomCollection(
        itemId: ItemIdDto,
        item: ShortItem?
    ): CollectionIdDto {
        val (token, tokenId) = CompositeItemIdParser.split(itemId.value)
        // ArtBlocks has tokenIds like 123003422, where last 6 digits - tokenId inside project
        // and remaining leading digits - projectId
        val projectId = tokenId.toLong() / 1000000
        val collectionId = CollectionIdDto(itemId.blockchain, token)

        // For 0 token no migration required
        if (projectId == 0L) {
            return collectionId
        }

        val subCollectionId = collectionId.copy(value = "${token}_$projectId")

        if (!artificialCollectionService.exists(subCollectionId)) {
            val name = customCollectionItemProvider.getMeta(listOf(itemId))[itemId]
                ?.attributes?.find { it.key == "collection_name" }?.value

            artificialCollectionService.createArtificialCollection(
                collectionId,
                subCollectionId,
                name,
                UnionCollection.Structure.PART
            )
        }
        return subCollectionId
    }
}
