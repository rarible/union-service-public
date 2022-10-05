package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import com.rarible.protocol.union.enrichment.validator.BestOrderValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * We have problem Union service could have Items in enrichment with outdated Orders (bestBid/bestSell).
 * In most cases it caused by lack of Order update events, which originally should be emitted by indexers.
 * In order to catch such situations, we want to run this job from time to time.
 */
@Component
class ReconciliationCorruptedItemJob(
    private val itemRepository: ItemRepository,
    private val orderRouter: BlockchainRouter<OrderService>,
    private val refreshService: EnrichmentRefreshService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val batchSize = 100

    fun reconcileCorruptedItems(fromShortItemId: ShortItemId?, blockchainDto: BlockchainDto): Flow<ShortItemId> {
        return flow {
            var next = fromShortItemId
            do {
                next = reconcileBatch(next, blockchainDto)
                if (next != null) {
                    emit(next)
                }
            } while (next != null)
        }
    }

    private suspend fun reconcileBatch(fromShortItemId: ShortItemId?, blockchainDto: BlockchainDto): ShortItemId? {
        val page = itemRepository.findByBlockchain(fromShortItemId, blockchainDto, batchSize).toList()

        // Linking all orders to their items
        val orderToItem = HashMap<OrderIdDto, ShortItemId>()
        page.forEach { item ->
            item.bestSellOrder?.let { orderToItem[it.dtoId] = item.id }
            item.bestBidOrder?.let { orderToItem[it.dtoId] = item.id }
        }

        // Looking for orders with incorrect status
        val bestOrders = getOrders(orderToItem.keys)
        val corruptedItems = HashSet<ShortItemId>()
        bestOrders.forEach {
            val itemId = orderToItem.remove(it.id)!!
            if (!BestOrderValidator.isValid(it)) {
                logger.info(
                    "Found best Order with incorrect state: {} (platform={}, updatedAt={}), Item: {}",
                    it.id, it.platform, it.lastUpdatedAt, itemId
                )
                corruptedItems.add(itemId)
            }
        }

        // Potentially there could be orders which doesn't exist anymore, such items should be reconciled too
        orderToItem.forEach {
            logger.info("Found non-existing Order: {}, Item: {}", it.key, it.value)
            corruptedItems.add(it.value)
        }

        corruptedItems.forEach {
            try {
                refreshService.reconcileItem(it.toDto(), true)
            } catch (e: Exception) {
                // In some cases reconciliation might fail (if item doesn't exist anymore in blockchain)
                // We should skip it in order to do not block job from reconciling other items
                logger.warn("Failed to reconcile item {}", it.toDto(), e)
            }
        }
        logger.info("Item batch refreshed, {}/{} were corrupted", corruptedItems.size, page.size)
        return page.lastOrNull()?.id
    }

    private suspend fun getOrders(ids: Collection<OrderIdDto>): List<OrderDto> {
        val groupedIds = ids.groupBy({ it.blockchain }, { it.value })
        return groupedIds.flatMap {
            orderRouter.getService(it.key).getOrdersByIds(it.value)
        }
    }
}