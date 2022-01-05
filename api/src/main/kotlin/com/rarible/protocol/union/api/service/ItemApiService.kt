package com.rarible.protocol.union.api.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgPage
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.configuration.MetaProperties
import com.rarible.protocol.union.enrichment.meta.IpfsUrlResolver
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentMetaService
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class ItemApiService(
    private val orderApiService: OrderApiService,
    private val enrichmentItemService: EnrichmentItemService,
    private val enrichmentMetaService: EnrichmentMetaService,
    private val metaProperties: MetaProperties,
    private val router: BlockchainRouter<ItemService>,
    private val ipfsUrlResolver: IpfsUrlResolver
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

    suspend fun getAvailableMetaOrScheduleAndWait(itemId: ItemIdDto): UnionMeta? =
        enrichmentMetaService.getAvailableMetaOrScheduleAndWait(
            itemId = itemId,
            loadingWaitTimeout = metaProperties.timeoutSyncLoadingMeta
        )

    private suspend fun enrich(unionItems: List<UnionItem>): List<ItemDto> {
        if (unionItems.isEmpty()) {
            return emptyList()
        }
        val now = nowMillis()
        val shortItems: Map<ItemIdDto, ShortItem> = enrichmentItemService
            .findAll(unionItems.map { ShortItemId(it.id) })
            .associateBy { it.id.toDto() }

        // Looking for full orders for existing items in order-indexer
        val shortOrderIds = shortItems.values
            .map { listOfNotNull(it.bestBidOrder?.dtoId, it.bestSellOrder?.dtoId) }
            .flatten()

        val orders = orderApiService.getByIds(shortOrderIds)
            .associateBy { it.id }

        val enrichedItems = unionItems.map {
            val shortItem = shortItems[it.id]
            enrichmentItemService.enrichItem(shortItem, it, orders)
        }

        logger.info(
            "Enriched {} of {} Items, {} Orders fetched ({}ms)",
            shortItems.size, enrichedItems.size, orders.size, spent(now)
        )

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
}
