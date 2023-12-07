package com.rarible.protocol.union.api.service.task

import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TaskSchedulingService(
    private val taskRepository: TaskRepository,
) {
    suspend fun schedule(type: String, param: String) {
        logger.info("Scheduling task type=$type, param=$param")
        val existingTask =
            taskRepository.findByTypeAndParam(type = type, param = param)
                .awaitFirstOrNull()
        if (existingTask != null) {
            taskRepository.delete(existingTask).awaitSingle()
        }
        taskRepository.save(
            Task(
                type = type,
                param = param,
                running = false
            )
        ).awaitSingle()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskSchedulingService::class.java)
    }
}
