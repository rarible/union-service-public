package com.rarible.protocol.union.api.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgPage
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.meta.UnionMetaService
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class ItemApiService(
    private val orderApiService: OrderApiService,
    private val enrichmentItemService: EnrichmentItemService,
    private val unionMetaService: UnionMetaService,
    private val router: BlockchainRouter<ItemService>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        cursor: String?,
        safeSize: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): List<ArgPage<UnionItem>> {
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map(BlockchainDto::name)
        val slices = getItemsByBlockchains(cursor, evaluatedBlockchains) { blockchain, continuation ->
            val blockDto = BlockchainDto.valueOf(blockchain)
            router.getService(blockDto).getAllItems(continuation, safeSize, showDeleted, lastUpdatedFrom, lastUpdatedTo)
        }
        return slices
    }

    suspend fun getItemsByIds(ids: List<ItemIdDto>): List<ItemDto> = coroutineScope {
        logger.info("Getting items by IDs: [{}]", ids.map { "${it.blockchain}:${it.value}" })
        val groupedIds = ids.groupBy({ it.blockchain }, { it.value })

        groupedIds.flatMap {
            router.getService(it.key).getItemsByIds(it.value)
        }.map {
            async {
                enrich(it)
            }
        }.awaitAll()
    }

    suspend fun enrich(unionItemsPage: Page<UnionItem>): ItemsDto {
        return ItemsDto(
            total = unionItemsPage.total,
            continuation = unionItemsPage.continuation,
            items = enrich(unionItemsPage.entities)
        )
    }

    suspend fun enrich(unionItemsSlice: Slice<UnionItem>, total: Long): ItemsDto {
        return ItemsDto(
            total = total,
            continuation = unionItemsSlice.continuation,
            items = enrich(unionItemsSlice.entities)
        )
    }

    suspend fun getAllItemIdsByCollection(collectionId: CollectionIdDto): Flow<ItemIdDto> {
        val pageSize = PageSize.ITEM.max
        var continuation: String? = null
        var returned = 0L
        return flow {
            while (true) {
                val page = router.getService(collectionId.blockchain)
                    .getItemsByCollection(collectionId.value, null, continuation, pageSize)
                page.entities.forEach { emit(it.id) }
                continuation = page.continuation
                if (continuation == null || page.total == 0) break
                returned += page.total
                check(returned < 1_000_000) { "Cyclic continuation $continuation for collection $collectionId" }
            }
        }
    }

    private suspend fun enrich(unionItems: List<UnionItem>): List<ItemDto> {
        if (unionItems.isEmpty()) {
            return emptyList()
        }
        val now = nowMillis()

        val enrichedItems = coroutineScope {

            val meta = async {
                unionMetaService.getAvailableMeta(unionItems.map { it.id })
            }

            val shortItems: Map<ItemIdDto, ShortItem> = enrichmentItemService
                .findAll(unionItems.map { ShortItemId(it.id) })
                .associateBy { it.id.toDto() }

            // Looking for full orders for existing items in order-indexer
            val shortOrderIds = shortItems.values
                .map { listOfNotNull(it.bestBidOrder?.dtoId, it.bestSellOrder?.dtoId) }
                .flatten()

            val orders = orderApiService.getByIds(shortOrderIds)
                .associateBy { it.id }

            val enriched = unionItems.map {
                val shortItem = shortItems[it.id]
                enrichmentItemService.enrichItem(
                    shortItem = shortItem,
                    item = it,
                    orders = orders,
                    meta = meta.await()
                )
            }
            logger.info("Enriched {} of {} Items ({}ms)", shortItems.size, unionItems.size, spent(now))
            enriched
        }


        return enrichedItems
    }

    private suspend fun getItemsByBlockchains(
        continuation: String?,
        blockchains: Collection<String>,
        clientCall: suspend (blockchain: String, continuation: String?) -> Page<UnionItem>
    ): List<ArgPage<UnionItem>> {
        val currentContinuation = CombinedContinuation.parse(continuation)
        return coroutineScope {
            blockchains.map { blockchain ->
                async {
                    val blockchainContinuation = currentContinuation.continuations[blockchain]
                    // For completed blockchain we do not request orders
                    if (blockchainContinuation == ArgSlice.COMPLETED) {
                        ArgPage(blockchain, blockchainContinuation, Page(0, null, emptyList()))
                    } else {
                        ArgPage(
                            blockchain,
                            blockchainContinuation,
                            clientCall.invoke(blockchain, blockchainContinuation)
                        )
                    }
                }
            }
        }.awaitAll()
    }

    suspend fun enrich(unionItem: UnionItem): ItemDto {
        val shortId = ShortItemId(unionItem.id)
        val shortItem = enrichmentItemService.get(shortId)
        return enrichmentItemService.enrichItem(shortItem, unionItem)
    }
}
