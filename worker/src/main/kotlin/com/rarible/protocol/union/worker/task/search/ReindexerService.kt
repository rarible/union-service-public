package com.rarible.protocol.union.worker.task.search

import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.elasticsearch.ReindexSchedulingService
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.config.SearchReindexerProperties
import com.rarible.protocol.union.worker.task.search.activity.ActivityTaskParam
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class ReindexerService(
    private val searchReindexerProperties : SearchReindexerProperties,
    private val taskRepository: TaskRepository,
    private val paramFactory: ParamFactory
) : ReindexSchedulingService {

    override suspend fun scheduleReindex(
        newIndexName: String,
        entityDefinition: EntityDefinitionExtended
    ) {
        when (entityDefinition.name){

            EsActivity.NAME -> {
                val blockchainDtoList = searchReindexerProperties.activityTasks.map { it.blockchainDto }
                val activityTypeDtos = searchReindexerProperties.activityTasks.map { it.type }
                scheduleActivityReindex(blockchainDtoList, activityTypeDtos, newIndexName)
            }
        }
    }

    suspend fun scheduleActivityReindex(
        blockchains: Collection<BlockchainDto>,
        activityTypes: Collection<ActivityTypeDto>,
        indexName: String
    ): List<Task> {
        logger.info(
            "Scheduling activity reindexing with params: blockchains={}, activityTypes={}, indexName={}",
            blockchains, activityTypes, indexName
        )

        val tasks = blockchains.zip(activityTypes).mapAsync { (blockchain, activity) ->
            task(blockchain, activity, indexName)
        }.filterNotNull()

        return taskRepository.saveAll(tasks).collectList().awaitFirst()
    }

    private suspend fun task(
        blockchain: BlockchainDto,
        activity: ActivityTypeDto,
        indexName: String
    ): Task? {
        val taskParam = ActivityTaskParam(blockchain, activity, indexName)
        val existing = taskRepository
            .findByTypeAndParam(EsActivity.ENTITY_DEFINITION.reindexTaskName, paramFactory.toString(taskParam))
            .awaitSingleOrNull()
        return if (existing == null) {
            Task(
                EsActivity.ENTITY_DEFINITION.reindexTaskName,
                "",
                taskParam,
                false
            )
        } else null
    }

    companion object {
        val logger by Logger()
    }
}