package com.rarible.protocol.union.worker.task.search.collection

import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class ChangeEsCollectionAliasTask(
    private val taskRepository: TaskRepository,
    private val esCollectionRepository: EsCollectionRepository,
    private val indexService: IndexService,
    private val paramFactory: ParamFactory
): TaskHandler<Unit> {
    override val type: String
        get() = TYPE

    private val entityDefinition = esCollectionRepository.entityDefinition

    override suspend fun isAbleToRun(param: String): Boolean {
        val parameter = paramFactory.parse<ChangeEsCollectionAliasTaskParam>(param)
        val tasks = parameter.blockchains.mapAsync { blockchain ->
            taskRepository.findByTypeAndParam(
                EsCollection.ENTITY_DEFINITION.reindexTask,
                blockchain.toString()
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
            esCollectionRepository.refresh()
            logger.info("Finished reindex of ${entityDefinition.entity} with index $param")
        }
    }

    companion object {
        const val TYPE = "CHANGE_ES_COLLECTION_ALIAS_TASK"
        private val logger by Logger()
    }
}