package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.core.logging.asyncWithTraceId
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CustomCollectionItemProvider(
    private val router: BlockchainRouter<ItemService>,
    private val itemRepository: ItemRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

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

    suspend fun getOrFetchMeta(itemIds: Collection<ItemIdDto>): Map<ItemIdDto, UnionMeta> {
        val fromDb = getMeta(itemIds)

        val missingFromDb = itemIds.filter { fromDb.containsKey(it) }
        val fromBlockchain = fetchMeta(missingFromDb)
        return fromDb + fromBlockchain
    }

    suspend fun getMeta(itemIds: Collection<ItemIdDto>): Map<ItemIdDto, UnionMeta> {
        return itemRepository.getAll(itemIds.map { ShortItemId(it) })
            .filter { it.metaEntry?.data != null }
            .associateBy({ it.id.toDto() }, { it.metaEntry!!.data!! })
    }

    suspend fun fetchMeta(itemIds: Collection<ItemIdDto>): Map<ItemIdDto, UnionMeta> {
        if (itemIds.isEmpty()) {
            return emptyMap()
        }
        logger.warn("Fetching meta to determine custom collections for Items: {}", itemIds.map { it.fullId() })
        // TODO Ideally it SHOULD NOT be used
        return coroutineScope {
            itemIds.chunked(16).map { chunk ->
                chunk.map {
                    asyncWithTraceId(context = NonCancellable) {
                        try {
                            it to router.getService(it.blockchain).getItemMetaById(it.value)
                        } catch (e: Exception) {
                            logger.warn(
                                "Failed to fetch meta to determine custom collection of Item: {}",
                                it.fullId()
                            )
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        }.flatten().associateBy({ it.first }, { it.second })
    }
}