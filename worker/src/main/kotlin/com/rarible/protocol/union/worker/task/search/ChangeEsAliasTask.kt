package com.rarible.protocol.union.worker.task.search

import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import com.rarible.protocol.union.core.elasticsearch.EsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.awaitSingleOrNull

open class ChangeEsAliasTask(
    override val type: String,
    private val entityDefinition: EntityDefinitionExtended,
    private val taskRepository: TaskRepository,
    private val esRepository: EsRepository,
    private val indexService: IndexService,
    private val paramFactory: ParamFactory
) : TaskHandler<Unit> {

    override suspend fun isAbleToRun(param: String): Boolean {
        val parameter = paramFactory.parse<ChangeAliasTaskParam>(param)
        val tasks = parameter.tasks.mapAsync { taskParam ->
            val task = taskRepository.findByTypeAndParam(
                entityDefinition.reindexTask,
                taskParam
            ).awaitSingleOrNull()
            logger.info("Search result of ${entityDefinition.reindexTask}, $taskParam = $task")
            task
        }

        return tasks.all { it?.lastStatus == TaskStatus.COMPLETED }
    }

    /**
     * @param param - ES index name
     */
    override fun runLongTask(from: Unit?, param: String): Flow<Unit> {
        return flow {
            val parameter = paramFactory.parse<ChangeAliasTaskParam>(param)
            indexService.finishIndexing(parameter.indexName, entityDefinition)
            esRepository.refresh()
            logger.info("Finished reindex of ${entityDefinition.entity} with index $param")
        }
    }

    companion object {
        private val logger by Logger()
    }
}