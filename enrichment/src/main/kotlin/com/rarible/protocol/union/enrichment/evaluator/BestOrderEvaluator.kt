package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortOrder
import org.slf4j.LoggerFactory
import java.math.BigDecimal

class BestOrderEvaluator(
    private val comparator: BestOrderComparator,
    private val provider: BestOrderProvider<*>
) {

    private val id = provider.entityId
    private val type = provider.entityType.simpleName
    private val name = comparator.name

    companion object {
        private val logger = LoggerFactory.getLogger(BestOrderEvaluator::class.java)
    }

    suspend fun evaluateBestOrder(current: ShortOrder?, updated: OrderDto): ShortOrder? {
        val shortUpdated = ShortOrderConverter.convert(updated)
        return if (isAlive(updated)) {
            onAliveOrderUpdate(current, shortUpdated)
        } else {
            onDeadOrderUpdate(current, shortUpdated)
        }
    }

    private fun onAliveOrderUpdate(current: ShortOrder?, updated: ShortOrder): ShortOrder {
        return if (current == null) {
            setBestOrder(updated)
        } else if (updated.id == current.id) {
            updateBestOrder(updated)
        } else {
            evaluateBestOrder(current, updated)
        }
    }

    private suspend fun onDeadOrderUpdate(current: ShortOrder?, updated: ShortOrder): ShortOrder? {
        return if (current == null) {
            skipDeadOrder(updated)
        } else if (updated.id == current.id) {
            refetchDeadOrder(updated)
        } else {
            ignoreDeadOrder(current, updated)
        }
    }

    //--- Methods below presented as separate methods mostly for logging ---//

    // Set alive best Order for entity if there is no current best Order
    private fun setBestOrder(updated: ShortOrder): ShortOrder {
        logger.info(
            "Updated {} Order [{}] is alive, current Order for {} [{}] is null - using updated Order",
            name, updated.getIdDto().fullId(), type, id
        )
        return updated
    }

    // Update current best Order with new data if current best Order is same as updated Order
    private fun updateBestOrder(updated: ShortOrder): ShortOrder {
        logger.info(
            "Updated {} Order [{}] is the same as current for {} [{}] - using updated Order",
            name, updated.getIdDto().fullId(), type, id
        )
        return updated
    }

    // Select best Order between current and updated if they are different and alive
    private fun evaluateBestOrder(current: ShortOrder, updated: ShortOrder): ShortOrder {
        val isCurrentPreferred = isPreferred(current)
        val isUpdatedPreferred = isPreferred(updated)

        val bestOrder = if (isCurrentPreferred != isUpdatedPreferred) {
            // if one of orders has preferred type and second hasn't return select preferred Order
            if (isCurrentPreferred) current else updated
        } else {
            // If both orders has preferred type or both are not preferred, comparing them
            comparator.compare(current, updated)
        }

        logger.info(
            "Evaluated {} for {} [{}] (current = [{}], updated = [{}], best = [{}])",
            name, type, id, current.getIdDto().fullId(), updated.getIdDto().fullId(), bestOrder.getIdDto().fullId()
        )
        return bestOrder
    }

    // Ignore dead Order when current best Order is not exist
    private fun skipDeadOrder(updated: ShortOrder): ShortOrder? {
        logger.info(
            "Updated {} Order [{}] is cancelled/filled, current Order for {} [{}] is null - nothing to update",
            name, updated.getIdDto().fullId(), type, id
        )
        return null
    }

    // Re-fetch best order from indexer if updated Order is dead and the same as current best Order
    private suspend fun refetchDeadOrder(updated: ShortOrder): ShortOrder? {
        logger.info(
            "Updated {} Order [{}] is cancelled/filled, current Order for {} [{}] is the same - dropping it",
            name, updated.getIdDto().fullId(), type, id
        )
        // It means, current best Order is not alive, we have to fetch actual best Order
        val fetched = provider.fetch()
        logger.info("Fetched {} for {} [{}] : [{}]", name, type, id, fetched?.id)
        return fetched?.let { ShortOrderConverter.convert(it) }
    }

    // Ignore dead Order when current best Order exists
    private fun ignoreDeadOrder(current: ShortOrder, updated: ShortOrder): ShortOrder {
        logger.info(
            "Updated {} Order [{}] is cancelled/filled, current Order for {} [{}] is [{}] - nothing to update",
            name, updated.getIdDto().fullId(), type, id, current.getIdDto().fullId()
        )
        return current
    }

    private fun isPreferred(order: ShortOrder): Boolean {
        return order.platform == PlatformDto.RARIBLE.name
    }

    private fun isAlive(order: OrderDto): Boolean {
        return order.take.value != order.fill && !order.cancelled && order.makeStock != BigDecimal.ZERO
    }

}