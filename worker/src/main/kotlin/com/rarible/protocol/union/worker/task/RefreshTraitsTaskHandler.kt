package com.rarible.protocol.union.worker.task

import com.rarible.core.logging.withTraceId
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.task.Tasks
import com.rarible.protocol.union.dto.BlockchainIdFormatException
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.service.TraitService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

@Component
class RefreshTraitsTaskHandler(
    private val traitService: TraitService,
    private val collectionRepository: CollectionRepository,
) : TaskHandler<String> {
    override val type = Tasks.REFRESH_TRAITS_TASK

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        try {
            logger.info("Starting RefreshTraitsJob(from=$from, param=$param)")
            if (param.isNotBlank()) {
                traitService.recalculateTraits(EnrichmentCollectionId.of(param))
            } else {
                val counter = AtomicLong()
                val lastId = AtomicReference(from?.let { EnrichmentCollectionId.of(it) })
                val shouldRun = AtomicReference(true)
                while (shouldRun.get()) {
                    val collections =
                        collectionRepository.findAll(fromIdExcluded = lastId.get(), limit = BATCH_SIZE).toList()
                    collections.forEach { collection ->
                        try {
                            logger.info("Processing collection ${collection.id}")
                            traitService.recalculateTraits(collection.id)
                        } catch (ex: Exception) {
                            logger.warn("Error processing collection for updating traits", ex)
                        }
                        if (counter.incrementAndGet() % BATCH_SIZE == 0L) {
                            emit(collection.id.toString())
                        }
                    }
                    lastId.set(collections.last().id)
                    shouldRun.set(collections.size == BATCH_SIZE)
                }
            }
            logger.info("Finished RefreshTraitsJob")
        } catch (e: BlockchainIdFormatException) {
            logger.warn("Unable to process job from=$from, param=$param", e)
        } catch (e: Throwable) {
            logger.error("Unable to finish job", e)
        }
    }.withTraceId()

    private val logger = LoggerFactory.getLogger(RefreshTraitsTaskHandler::class.java)

    companion object {
        const val BATCH_SIZE = 1000
    }
}
