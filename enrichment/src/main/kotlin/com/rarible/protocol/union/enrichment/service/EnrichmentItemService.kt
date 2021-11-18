package com.rarible.protocol.union.enrichment.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.APP)
class EnrichmentItemService(
    private val itemServiceRouter: BlockchainRouter<ItemService>,
    private val itemRepository: ItemRepository,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val enrichmentAuctionService: EnrichmentAuctionService,
    private val enrichmentMetaService: EnrichmentMetaService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentItemService::class.java)
    private val FETCH_SIZE = 1_000

    suspend fun get(itemId: ShortItemId): ShortItem? {
        return itemRepository.get(itemId)
    }

    suspend fun getOrEmpty(itemId: ShortItemId): ShortItem {
        return itemRepository.get(itemId) ?: ShortItem.empty(itemId)
    }

    suspend fun save(item: ShortItem): ShortItem {
        return itemRepository.save(item.withCalculatedFields())
    }

    suspend fun delete(itemId: ShortItemId): DeleteResult? {
        val now = nowMillis()
        val result = itemRepository.delete(itemId)
        logger.info("Deleting Item [{}], deleted: {} ({}ms)", itemId, result?.deletedCount, spent(now))
        return result
    }

    suspend fun findAll(ids: List<ShortItemId>): List<ShortItem> {
        return itemRepository.findAll(ids)
    }

    // TODO UNION - we need to get rid of usage of 'owners' field
    fun findByCollection(address: ContractAddress, owner: UnionAddress? = null): Flow<ShortItemId> = flow {
        var continuation: String? = null
        logger.info("Fetching all items for collection {} and owner {}", address, owner)
        var count = 0
        do {
            val page = itemServiceRouter.getService(address.blockchain)
                .getItemsByCollection(address.value, continuation, FETCH_SIZE)
            page.entities
                .filter { item -> owner?.let { item.owners.contains(it) } ?: true }
                .map { ShortItemId(it.id) }.forEach { emit(it) }
            count += page.entities.count()
            continuation = page.continuation
        } while (continuation != null)
        logger.info("Fetched {} items for collection {} and owner {}", count, address, owner)
    }

    fun findByAuctionId(auctionIdDto: AuctionIdDto) = itemRepository.findWithAuction(auctionIdDto)

    suspend fun fetch(itemId: ShortItemId): UnionItem {
        val now = nowMillis()
        val nftItemDto = itemServiceRouter.getService(itemId.blockchain)
            .getItemById(itemId.toDto().value)

        logger.info("Fetched Item by Id [{}] ({}ms)", itemId, spent(now))
        return nftItemDto
    }

    // [orders] is a set of already fetched orders that can be used as cache to avoid unnecessary 'getById' calls
    suspend fun enrichItem(
        shortItem: ShortItem?,
        item: UnionItem? = null,
        orders: Map<OrderIdDto, OrderDto> = emptyMap(),
        auctions: Map<AuctionIdDto, AuctionDto> = emptyMap()
    ) = coroutineScope {
        require(shortItem != null || item != null)
        val fetchedItem = async { item ?: fetch(shortItem!!.id) }
        val bestSellOrder = async { enrichmentOrderService.fetchOrderIfDiffers(shortItem?.bestSellOrder, orders) }
        val bestBidOrder = async { enrichmentOrderService.fetchOrderIfDiffers(shortItem?.bestBidOrder, orders) }
        val meta = async {
            val itemId = shortItem?.id ?: ShortItemId(item!!.id)
            enrichmentMetaService.enrichMeta(item?.meta ?: fetchedItem.await().meta, itemId)
        }

        val bestOrders = listOf(bestSellOrder, bestBidOrder)
            .awaitAll().filterNotNull()
            .associateBy { it.id }

        val auctionIds = when {
            shortItem != null -> shortItem.auctions
            auctions.isNotEmpty() -> auctions.keys
            else -> emptySet()
        }
        val auctionsData = async { enrichmentAuctionService.fetchAuctionsIfAbsent(auctionIds, auctions) }

        EnrichedItemConverter.convert(fetchedItem.await(), shortItem, meta.await(), bestOrders, auctionsData.await())
    }
}
