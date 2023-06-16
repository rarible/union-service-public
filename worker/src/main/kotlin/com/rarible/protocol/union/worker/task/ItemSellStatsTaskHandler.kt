package com.rarible.protocol.union.worker.task

import com.rarible.core.common.optimisticLock
import com.rarible.core.logging.withTraceId
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ItemSellStats
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemSellStatsTaskHandler(
    private val enrichmentOwnershipService: EnrichmentOwnershipService,
    private val itemRepository: ItemRepository,
) : TaskHandler<String> {
    override val type = "ITEM_SELL_STATS_TASK"

    override fun runLongTask(from: String?, param: String): Flow<String> =
        if (param.isBlank()) {
            itemRepository.findAll(fromIdExcluded = from?.let { ShortItemId(IdParser.parseItemId(from)) }).map {
                updateStats(it.id)
                it.id.toString()
            }
        } else {
            flow {
                updateStats(ShortItemId(IdParser.parseItemId(param)))
            }
        }.withTraceId()

    private suspend fun updateStats(id: ShortItemId) {
        optimisticLock {
            val item = itemRepository.get(id) ?: return@optimisticLock
            val stats = enrichmentOwnershipService.getItemSellStats(id)
            if (ItemSellStats(sellers = item.sellers, totalStock = item.totalStock) != stats) {
                logger.info("Updating item $id stats to $stats")
                itemRepository.save(item.copy(sellers = stats.sellers, totalStock = stats.totalStock))
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ItemSellStatsTaskHandler::class.java)
    }
}