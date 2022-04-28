package com.rarible.protocol.union.worker.task.search

import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.task.search.activity.ActivityTask
import com.rarible.protocol.union.worker.task.search.activity.ActivityTaskParam
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class ReindexerService(
    private val taskRepository: TaskRepository,
    private val paramFactory: ParamFactory
) {

    suspend fun scheduleActivityReindex(
        blockchains: Collection<BlockchainDto>,
        activityTypes: Collection<ActivityTypeDto>,
        indexName: String
    ): List<Task> {
        logger.info(
            "Scheduling activity reindexing with params: blockchains={}, activityTypes={}, indexName={}",
            blockchains, activityTypes, indexName
        )

        val tasks = blockchains.zip(activityTypes).mapAsync { (blockchain, activity) ->
            val taskParam = ActivityTaskParam(blockchain, activity, indexName)
            val existing = taskRepository
                .findByTypeAndParam(ActivityTask.ACTIVITY_REINDEX, paramFactory.toString(taskParam))
                .awaitSingleOrNull()
            if(existing == null) {
                Task(
                    ActivityTask.ACTIVITY_REINDEX,
                    "",
                    taskParam,
                    false
                )
            } else null
        }.filterNotNull()

        return taskRepository.saveAll(tasks).collectList().awaitFirst()
    }

    companion object {
        val logger by Logger()
    }
}