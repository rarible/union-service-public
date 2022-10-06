package com.rarible.protocol.union.api.service.elastic

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.core.task.ActivityTaskParam
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ElasticMaintenanceService(
    private val taskRepository: TaskRepository,
    private val objectMapper: ObjectMapper,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val ACTIVITY_REINDEX_TASK_TYPE = "ACTIVITY_REINDEX"
        private const val MAINTENANCE_TAG = "MAINTENANCE"
    }

    suspend fun scheduleReindexActivitiesTasks(
        blockchains: List<BlockchainDto> = emptyList(),
        types: List<ActivityTypeDto> = emptyList(),
        from: Long? = null,
        to: Long? = null,
        esIndex: String,
        deletePreviousTasks: Boolean = true,
    ) {
        if (deletePreviousTasks) {
            deletePreviousTasks(ACTIVITY_REINDEX_TASK_TYPE)
        }

        val tasks = mutableListOf<Task>()

        val actualBlockchains = blockchains.ifEmpty {
            BlockchainDto.values().toList()
        }
        val actualTypes = types.ifEmpty {
            ActivityTypeDto.values().toList()
        }

        actualBlockchains.forEach { blockchain ->
            actualTypes.forEach { type ->
                val param = ActivityTaskParam(
                    blockchain = blockchain,
                    type = type,
                    from = from,
                    to = to,
                    index = esIndex,
                    settingsHash = null,
                    versionData = null,
                    tags = listOf(MAINTENANCE_TAG)
                )
                val task = Task(
                    type = ACTIVITY_REINDEX_TASK_TYPE,
                    param = objectMapper.writeValueAsString(param),
                    running = false,
                )
                tasks.add(task)
            }
        }

        logger.info("Scheduling ${tasks.size} maintenance tasks for reindexing activities")
        taskRepository.saveAll(tasks).collectList().awaitFirst()
    }

    private suspend fun deletePreviousTasks(type: String) {
        val previousTasks = taskRepository.findByTypeAndParamRegex(type, MAINTENANCE_TAG)
            .collectList().awaitFirst()
        logger.info("Found ${previousTasks.size} previous maintenance tasks of type $type")

        val taskIdsToDelete = previousTasks.filter { task ->
            val param = objectMapper.readValue(task.param, ParamWithTags::class.java)
            param.tags?.contains(MAINTENANCE_TAG) ?: false // Double-check if regex matched something else
        }.map { it.id.toString() }

        logger.info("Deleting previous maintenance tasks: $taskIdsToDelete")
        taskRepository.deleteAllById(taskIdsToDelete).awaitFirstOrNull()
    }

    private data class ParamWithTags(
        val tags: List<String>? = null
    )
}
