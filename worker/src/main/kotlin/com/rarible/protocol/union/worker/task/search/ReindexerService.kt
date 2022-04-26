package com.rarible.protocol.union.worker.task.search

import com.rarible.core.logging.Logger
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.task.search.activity.ActivityTask
import com.rarible.protocol.union.worker.task.search.activity.ActivityTaskParam
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component

@Component
class ReindexerService(
    private val taskRepository: TaskRepository
) {

    suspend fun scheduleActivityReindex(
        blockchain: BlockchainDto,
        activityType: ActivityTypeDto,
        indexName: String,
        cursor: String? = null
    ): Task {
        logger.info(
            "Scheduling activity reindexing with params: blockchain={}, activityType={}, indexName={}, cursor={}",
            blockchain, activityType, indexName, cursor
        )
        return taskRepository.save(
            Task(
                ActivityTask.ACTIVITY_REINDEX,
                "",
                ActivityTaskParam(blockchain, activityType, indexName, cursor),
                false
            )
        ).awaitSingle()
    }

    suspend fun scheduleActivityReindex(
        blockchains: Collection<BlockchainDto>,
        activityTypes: Collection<ActivityTypeDto>,
        indexName: String
    ): List<Task> {
        logger.info(
            "Scheduling activity reindexing with params: blockchains={}, activityTypes={}, indexName={}",
            blockchains, activityTypes, indexName
        )

        val tasks = blockchains.zip(activityTypes).map { (blockchain, activity) ->
            Task(
                ActivityTask.ACTIVITY_REINDEX,
                "",
                ActivityTaskParam(blockchain, activity, indexName),
                false
            )
        }

        return taskRepository.saveAll(tasks).collectList().awaitFirst()
    }

    companion object {
        val logger by Logger()
    }
}