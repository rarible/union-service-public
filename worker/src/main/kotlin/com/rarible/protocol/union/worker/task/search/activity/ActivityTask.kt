package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.core.logging.Logger
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.worker.config.ActivityReindexProperties
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onCompletion

class ActivityTask(
    private val properties: ActivityReindexProperties,
    private val paramFactory: ParamFactory,
    private val activityReindexService: ActivityReindexService,
    private val repository: EsActivityRepository,
    private val indexService: IndexService,
): TaskHandler<String> {
    private val entityDefinition = repository.entityDefinition

    override val type: String
        get() = EsActivity.ENTITY_DEFINITION.reindexTask

    override suspend fun isAbleToRun(param: String): Boolean {
        val blockchain = paramFactory.parse<ActivityTaskParam>(param).blockchain
        return properties.enabled && properties.blockchains.single { it.blockchain == blockchain }.enabled
    }

    /**
     * from - cursor
     * param looks like ACTIVITY_ETHEREUM_LIST
     */
    override fun runLongTask(from: String?, param: String): Flow<String> {
        return if(from == "") {
            emptyFlow()
        } else {
            val taskParam = paramFactory.parse<ActivityTaskParam>(param)
            return activityReindexService
                .reindex(
                    blockchain = taskParam.blockchain,
                    type = taskParam.type,
                    index = taskParam.index,
                    cursor = from
                )
                .onCompletion {
                    indexService.finishIndexing(taskParam.index, entityDefinition)
                    repository.refresh()
                    logger.info("Finished reindex of ${entityDefinition.entity} with param $param")
                }
        }
    }

    companion object {
        private val logger by Logger()
    }
}
