package com.rarible.protocol.union.search.reindexer.task

import com.rarible.core.common.mapAsync
import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.search.reindexer.config.SearchReindexerConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingleOrNull


class ActivityTask(
    val config: SearchReindexerConfiguration,
    val activityClient: ActivityControllerApi,
    val taskRepository: TaskRepository
): TaskHandler<String> {
    override val type: String
        get() = ACTIVITY_REINDEX

    private val tasks = config.properties.activityTasks.associateBy { it.taskParam() }

    override fun getAutorunParams(): List<RunTask> {
        return config.properties.activityTasks.map {
            RunTask(it.taskParam())
        }
    }

    override suspend fun isAbleToRun(param: String): Boolean {
        return !allCompleted()
    }

    private suspend fun allCompleted(): Boolean {
        tasks.forEach { (param, _) ->
            val t = taskRepository.findByTypeAndParam(ACTIVITY_REINDEX, param).awaitSingleOrNull()
            if(t?.lastStatus != TaskStatus.COMPLETED) {
                return false
            }
        }
        return true
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val task = tasks[param]
        return if(task == null || from == "") {
            emptyFlow()
        } else {
            flow {
                val res = activityClient.getAllActivities(
                    listOf(task.type),
                    listOf(task.blockchainDto),
                    from,
                    from,
                    1000,
                    ActivitySortDto.EARLIEST_FIRST
                ).awaitFirst()

                emit(res.continuation ?: "")
            }
        }
    }



    companion object {
        private const val ACTIVITY_REINDEX = "ACTIVITY_REINDEX"
    }
}