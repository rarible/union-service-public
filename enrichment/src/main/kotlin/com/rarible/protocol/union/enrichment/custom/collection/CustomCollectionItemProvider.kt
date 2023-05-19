package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class CustomCollectionItemProvider(
    private val router: BlockchainRouter<ItemService>
) {

    suspend fun fetch(collectionId: CollectionIdDto, continuation: String?, size: Int): List<UnionItem> {
        return router.getService(collectionId.blockchain).getItemsByCollection(
            collection = collectionId.value,
            owner = null,
            continuation = continuation,
            size = size
        ).entities
    }

    suspend fun fetch(itemIds: List<ItemIdDto>): List<UnionItem> {
        if (itemIds.isEmpty()) {
            return emptyList()
        }
        return coroutineScope {
            val groupedIds = itemIds.groupBy({ it.blockchain }, { it.value })
            coroutineScope {
                groupedIds.map {
                    async { router.getService(it.key).getItemsByIds(it.value) }
                }.awaitAll().flatten()
            }
        }
    }

    suspend fun getItemCollectionId(itemId: ItemIdDto): CollectionIdDto? {
        return router.getService(itemId.blockchain).getItemCollectionId(itemId.value)
            ?.let { CollectionIdDto(itemId.blockchain, it) }
    }

}