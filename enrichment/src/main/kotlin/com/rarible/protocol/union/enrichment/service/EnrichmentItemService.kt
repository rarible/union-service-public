package com.rarible.protocol.union.enrichment.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.UnionItemDto
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentItemService(
    private val itemServiceRouter: BlockchainRouter<ItemService>,
    private val itemRepository: ItemRepository,
    private val enrichmentOrderService: EnrichmentOrderService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentItemService::class.java)

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

    suspend fun fetch(itemId: ShortItemId): UnionItemDto {
        val now = nowMillis()
        val nftItemDto = itemServiceRouter.getService(itemId.blockchain)
            .getItemById(itemId.toDto().value)

        logger.info("Fetched Item by Id [{}] ({}ms)", itemId, spent(now))
        return nftItemDto
    }

    // Here we could specify Order already fetched (or received via event) to avoid unnecessary getById call
    // if one of Item's short orders has same hash
    suspend fun enrichItem(shortItem: ShortItem, item: UnionItemDto? = null, order: OrderDto? = null) = coroutineScope {
        val fetchedItem = async { item ?: fetch(shortItem.id) }
        val bestSellOrder = async { enrichmentOrderService.fetchOrderIfDiffers(shortItem.bestSellOrder, order) }
        val bestBidOrder = async { enrichmentOrderService.fetchOrderIfDiffers(shortItem.bestBidOrder, order) }

        val orders = listOf(bestSellOrder, bestBidOrder)
            .mapNotNull { it.await() }
            .associateBy { it.id }

        EnrichedItemConverter.convert(fetchedItem.await(), shortItem, orders)
    }
}
