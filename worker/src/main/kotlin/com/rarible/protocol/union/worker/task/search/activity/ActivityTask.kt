package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.worker.config.SearchReindexerConfiguration
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst

class ActivityTask(
    private val config: SearchReindexerConfiguration,
    private val activityClient: ActivityControllerApi,
    private val esActivityRepository: EsActivityRepository,
    private val converter: EsActivityConverter,
    private val paramFactory: ParamFactory
): TaskHandler<String> {
    override val type: String
        get() = ACTIVITY_REINDEX

    override fun getAutorunParams(): List<RunTask> {
        return config.properties.activityTasks.map {
            RunTask(it.taskParam())
        }
    }

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
            val param = paramFactory.parse<ActivityTaskParam>(param)
            flow {
                val res = activityClient.getAllActivities(
                    listOf(param.activityType),
                    listOf(param.blockchain),
                    from,
                    from,
                    PAGE_SIZE,
                    ActivitySortDto.EARLIEST_FIRST
                ).awaitFirst()

                esActivityRepository.saveAll(
                    res.activities.mapNotNull(converter::convert),
                    param.index
                )

                emit(res.cursor ?: "")
            }
        }
    }

    companion object {
        const val ACTIVITY_REINDEX = "ACTIVITY_REINDEX"
        const val PAGE_SIZE = 1000
    }
}
