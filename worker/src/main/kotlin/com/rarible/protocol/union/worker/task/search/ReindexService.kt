package com.rarible.protocol.union.worker.task.search

import com.rarible.core.logging.Logger
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.union.core.elasticsearch.ReindexSchedulingService
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.task.search.activity.ActivityTaskParam
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class ReindexService(
    private val taskRepository: TaskRepository,
    private val paramFactory: ParamFactory
) : ReindexSchedulingService {

    override suspend fun scheduleReindex(
        newIndexName: String,
        entityDefinition: EntityDefinitionExtended
    ) {
        when (entityDefinition.entity) {
            EsEntity.ACTIVITY -> {
                scheduleActivityReindex(newIndexName)
            }
            EsEntity.ORDER,
            EsEntity.COLLECTION,
            EsEntity.OWNERSHIP -> {
                throw UnsupportedOperationException("Unsupported entity ${entityDefinition.entity} reindex")
            }
        }
    }

    suspend fun scheduleActivityReindex(indexName: String): List<Task> {
        val blockchains = BlockchainDto.values()
        val types = ActivityTypeDto.values()
        val tasks = blockchains.zip(types).mapNotNull { (blockchain, type) -> task(blockchain, type, indexName) }
        return taskRepository.saveAll(tasks).collectList().awaitFirst()
    }

    private suspend fun task(
        blockchain: BlockchainDto,
        type: ActivityTypeDto,
        index: String
    ): Task? {
        val taskParam = paramFactory.toString(ActivityTaskParam(blockchain, type, index))
        val taskType = EsActivity.ENTITY_DEFINITION.reindexTask

        val existing = taskRepository
            .findByTypeAndParam(taskType, taskParam)
            .awaitSingleOrNull()
        return if (existing == null) {
            logger.info(
                "Scheduling activity reindexing: blockchains={}, type={}, indexName={}",
                blockchain, type, index
            )
            Task(
                type = taskType,
                param = taskParam,
                state = null,
                running = false,
                lastStatus = TaskStatus.NONE
            )
        } else null
    }

    companion object {
        val logger by Logger()
    }
}