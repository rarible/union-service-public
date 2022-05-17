package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.elasticsearch.repository.EsActivityRepository
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class ChangeEsActivityAliasTask(
    private val taskRepository: TaskRepository,
    private val esActivityRepository: EsActivityRepository,
    private val indexService: IndexService,
    private val paramFactory: ParamFactory
): TaskHandler<Unit> {
    override val type: String
        get() = TYPE

    private val entityDefinition = esActivityRepository.entityDefinition

    override suspend fun isAbleToRun(param: String): Boolean {
        val parameter = paramFactory.parse<ChangeEsActivityAliasTaskParam>(param)
        val tasks = parameter.tasks.mapAsync { taskParam ->
            taskRepository.findByTypeAndParam(
                EsActivity.ENTITY_DEFINITION.reindexTask,
                paramFactory.toString(taskParam)
            ).awaitSingleOrNull()
        }.filterNotNull()

        return tasks.all { it.lastStatus == TaskStatus.COMPLETED }
    }

    /**
     * @param param - ES index name
     */
    override fun runLongTask(from: Unit?, param: String): Flow<Unit> {
        return flow {
            indexService.finishIndexing(param, entityDefinition)
            esActivityRepository.refresh()
            logger.info("Finished reindex of ${entityDefinition.entity} with index $param")
        }
    }

    companion object {
        const val TYPE = "CHANGE_ES_ACTIVITY_ALIAS_TASK"
        private val logger by Logger()
    }
}