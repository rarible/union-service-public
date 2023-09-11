package com.rarible.protocol.union.worker.task.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.logging.withTraceId
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class RefreshMetaTask(
    private val router: BlockchainRouter<ItemService>,
    private val itemRepository: ItemRepository,
    private val objectMapper: ObjectMapper,
    private val itemMetaService: ItemMetaService,
) : TaskHandler<String> {
    override val type = META_REFRESH_TASK

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        logger.info("Starting RefreshMetaTask from=$from, param=$param")
        val parsedParam = objectMapper.readValue(param, RefreshMetaTaskParam::class.java)
        val currentContinuation = AtomicReference(from)
        val collectionId = IdParser.parseCollectionId(parsedParam.collectionId)
        do {
            val items = router.getService(collectionId.blockchain).getItemsByCollection(
                collection = collectionId.value,
                owner = null,
                continuation = currentContinuation.get(),
                size = 1000,
            )
            val shortItems = itemRepository.getAll(items.entities.map { ShortItemId(it.id) })
            shortItems.forEach { item ->
                if (parsedParam.full || item.metaEntry == null || item.metaEntry?.isDownloaded() == false) {
                    logger.info("Reset meta for item ${item.id}")
                    itemMetaService.schedule(
                        itemId = item.id.toDto(),
                        pipeline = ItemMetaPipeline.REFRESH,
                        force = true,
                        priority = parsedParam.priority
                    )
                }
            }
            currentContinuation.set(items.continuation)
            if (items.continuation != null) {
                logger.info("Processed ${items.entities.size} items. Continuation: ${items.continuation}")
                emit(items.continuation!!)
            }
        } while (currentContinuation.get() != null)
        logger.info("Finished RefreshMetaTask from=$from, param=$param")
    }.withTraceId()

    companion object {
        const val META_REFRESH_TASK = "META_REFRESH_TASK"
        private val logger = LoggerFactory.getLogger(RefreshMetaTask::class.java)
    }
}

data class RefreshMetaTaskParam(
    val collectionId: String,
    val full: Boolean,
    val priority: Int = 0
)
