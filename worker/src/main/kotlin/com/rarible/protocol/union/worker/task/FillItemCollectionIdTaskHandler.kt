package com.rarible.protocol.union.worker.task

import com.rarible.core.common.optimisticLock
import com.rarible.core.logging.withTraceId
import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class FillItemCollectionIdTaskHandler(
    private val itemRepository: ItemRepository,
    private val enrichmentItemService: EnrichmentItemService,
) : TaskHandler<String> {
    override val type: String = "FILL_ITEM_COLLECTION_ID"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(""))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        logger.info("Starting FillItemCollectionIdTaskHandler from=$from")
        val progress = AtomicLong(0)
        itemRepository.findAll(fromIdExcluded = from?.let { ShortItemId.of(from) }).collect {
            progress.incrementAndGet()
            if (it.blockchain == BlockchainDto.SOLANA) {
                emit(it.id.toString())
                return@collect
            }
            optimisticLock {
                val forUpdate = itemRepository.get(it.id) ?: return@optimisticLock
                val contractId = forUpdate.itemId.split(":")[0]
                val customCollectionId = enrichmentItemService.resolveCustomCollection(forUpdate)?.value
                val collectionId = customCollectionId ?: contractId
                if (forUpdate.collectionId != collectionId) {
                    enrichmentItemService.save(forUpdate.copy(collectionId = collectionId))
                }
            }
            val currentProgress = progress.get()
            if (currentProgress % 10000 == 0) {
                logger.info("Processed $currentProgress items. Last: ${it.id}")
            }
            emit(it.id.toString())
        }
        logger.info("Finished FillItemCollectionIdTaskHandler from=$from")
    }.withTraceId()

    companion object {
        private val logger = LoggerFactory.getLogger(FillItemCollectionIdTaskHandler::class.java)
    }
}
