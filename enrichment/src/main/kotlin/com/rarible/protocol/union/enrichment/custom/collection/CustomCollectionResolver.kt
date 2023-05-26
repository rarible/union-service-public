package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.custom.collection.mapper.CollectionMapperIndex
import com.rarible.protocol.union.enrichment.model.ShortItem
import org.springframework.stereotype.Component

@Component
class CustomCollectionResolver(
    private val itemServiceRouter: BlockchainRouter<ItemService>,
    private val collectionMapperIndex: CollectionMapperIndex
) {

    suspend fun <T> resolve(
        entities: List<CustomCollectionResolutionRequest<T>>,
        hint: Map<ItemIdDto, ShortItem> = emptyMap()
    ): Map<T, CollectionIdDto> {
        val result = HashMap<T, CollectionIdDto>()
        val remain = entities.filter { e ->
            // Checking collections first - there can be only direct mapping for them
            if (e.itemId == null) {
                e.collectionId?.let { id -> resolve(id)?.let { result[e.entityId] = it } }
                false
            } else {
                // Then check is there is direct item mapping
                val customCollectionId = collectionMapperIndex.getItemMapping(e.itemId)
                if (customCollectionId != null) {
                    result[e.entityId] = customCollectionId
                    false
                } else {
                    true
                }
            }
        }

        // Resolving other item's collections
        val resolved = resolve(remain.map { it.itemId!! }, hint)

        remain.forEach { entity ->
            resolved[entity.itemId!!]?.let { result[entity.entityId] = it }
        }

        return result
    }

    private suspend fun resolve(
        itemIds: Collection<ItemIdDto>,
        hint: Map<ItemIdDto, ShortItem>
    ): Map<ItemIdDto, CollectionIdDto> {
        val gropedByCollection = itemIds.mapNotNull { itemId ->
            if (itemId.blockchain == BlockchainDto.SOLANA) {
                null
            } else {
                itemServiceRouter.getService(itemId.blockchain)
                    .getItemCollectionId(itemId.value)?.let { CollectionIdDto(itemId.blockchain, it) to itemId }
            }
        }.groupBy({ it.first }, { it.second })

        val result = HashMap<ItemIdDto, CollectionIdDto>()

        gropedByCollection.forEach { (collectionId, collectionItemIds) ->
            collectionMapperIndex.getCollectionMapper(collectionId)
                ?.getCustomCollections(collectionItemIds, hint)
                ?.let { result.putAll(it) }

        }

        return result
    }

    private suspend fun resolve(collectionId: CollectionIdDto): CollectionIdDto? {
        return collectionMapperIndex.getCollectionMapper(collectionId)
            ?.getCustomCollections(listOf(collectionId))
            ?.get(collectionId)
    }
}