package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.core.logging.Logger
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.worker.config.SearchReindexerConfiguration
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.reactive.awaitFirst
import scala.annotation.meta.param

class ActivityTask(
    private val config: SearchReindexerConfiguration,
    private val activityClient: ActivityControllerApi,
    private val paramFactory: ParamFactory,
    private val converter: EsActivityConverter,
    private val repository: EsActivityRepository,
    private val indexService: IndexService,

    ) : TaskHandler<String> {

    private val entityDefinition = repository.entityDefinition

    override val type: String
        get() = entityDefinition.reindexTaskName

    override suspend fun isAbleToRun(param: String): Boolean {
        return config.properties.startReindexActivity
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
            flow {
                val res = fetch(taskParam, from)
                repository.saveAll(res.activities.mapNotNull(converter::convert))
                logger.info("Reindex of ${entityDefinition.name} entities ${res.activities.size} continuation ${res.continuation}")
                emit(res.cursor ?: "")
            }.onCompletion {
                indexService.finishIndexing(taskParam.index, entityDefinition)
                repository.refresh()
                logger.info("Finished reindex of ${entityDefinition.name} with param $param")
            }
        }
    }

    private suspend fun fetch(
        taskParam: ActivityTaskParam,
        from: String?
    ) = activityClient.getAllActivities(
        listOf(taskParam.activityType),
        listOf(taskParam.blockchain),
        from,
        from,
        PAGE_SIZE,
        ActivitySortDto.EARLIEST_FIRST
    ).awaitFirst()

    companion object {
        const val PAGE_SIZE = 1000
        private val logger by Logger()
    }
}
