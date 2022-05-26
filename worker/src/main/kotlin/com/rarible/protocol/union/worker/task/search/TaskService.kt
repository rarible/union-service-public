package com.rarible.protocol.union.worker.task.search;

import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service;

@Service
class TaskService(
    private val taskRepository: TaskRepository
) {

    suspend fun <T> createTask(
        taskType: String,
        parameter: T,
        parameterSerializer: (T) -> String
    ): Task? {
        val taskParamStr = parameterSerializer(parameter)

        val existing = taskRepository
            .findByTypeAndParam(taskType, taskParamStr)
            .awaitSingleOrNull()
        return if (existing == null) {
            Task(
                type = taskType,
                param = taskParamStr,
                state = null,
                running = false,
                lastStatus = TaskStatus.NONE
            )
        } else {
            null
        }
    }

}
