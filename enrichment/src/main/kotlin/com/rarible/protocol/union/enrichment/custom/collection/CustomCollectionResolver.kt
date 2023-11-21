package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.custom.collection.mapper.CollectionMapperIndex
import com.rarible.protocol.union.enrichment.custom.collection.provider.CustomCollectionProvider
import com.rarible.protocol.union.enrichment.model.ShortItem
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

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
            val itemId = e.itemId
            // Checking collections first - there can be only direct mapping for them
            if (itemId == null) {
                e.collectionId?.let { id -> resolve(id)?.let { result[e.entityId] = it } }
                false
            } else {
                // Then check is there is direct item mapping
                val provider = collectionMapperIndex.getItemProvider(itemId)
                provider?.let { result[e.entityId] = it.getCustomCollection(itemId, hint[itemId]) }
                provider == null
            }
        }

        // Resolving other item's collections
        val resolved = resolve(remain.map { it.itemId!! }, ConcurrentHashMap(hint))

        remain.forEach { entity ->
            resolved[entity.itemId!!]?.let { result[entity.entityId] = it }
        }

        return result
    }

    private suspend fun resolve(
        itemIds: Collection<ItemIdDto>,
        hint: ConcurrentMap<ItemIdDto, ShortItem>
    ): Map<ItemIdDto, CollectionIdDto> {
        val gropedByCollection = itemIds.mapNotNull { itemId ->
            if (itemId.blockchain == BlockchainDto.SOLANA) {
                null
            } else {
                itemServiceRouter.getService(itemId.blockchain)
                    .getItemCollectionId(itemId.value)?.let { CollectionIdDto(itemId.blockchain, it) to itemId }
            }
        }.groupBy({ it.first }, { it.second })

        val result = HashMap<ItemIdDto, CustomCollectionProvider>()

        gropedByCollection.forEach { (collectionId, collectionItemIds) ->
            collectionMapperIndex.getCollectionMapper(collectionId)
                ?.getCustomCollectionProviders(collectionItemIds, hint)
                ?.let { result.putAll(it) }
        }

        return result.mapValues { it.value.getCustomCollection(it.key, hint[it.key]) }
    }

    private suspend fun resolve(collectionId: CollectionIdDto): CollectionIdDto? {
        return collectionMapperIndex.getCollectionMapper(collectionId)
            ?.getCustomCollectionProviders(listOf(collectionId))
            ?.get(collectionId)
            ?.getCustomCollection(collectionId, null)
    }
}
