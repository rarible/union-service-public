package com.rarible.protocol.union.listener.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.enrichment.download.DownloadTask
import com.rarible.protocol.union.enrichment.repository.DownloadTaskRepository
import com.rarible.protocol.union.enrichment.service.DownloadTaskService
import com.rarible.protocol.union.enrichment.test.data.randomDownloadTask
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@IntegrationTest
class DownloadTaskServiceIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var downloadTaskService: DownloadTaskService

    @Autowired
    lateinit var downloadTaskRepository: DownloadTaskRepository

    @Test
    fun `update - ok`() = runBlocking<Unit> {
        val taskSamePriority = createTask(priority = 10)
        val taskLowerPriority = createTask(priority = 0)
        val taskHigherPriority = createTask(priority = 20)
        val taskInProgress = createTask(inProgress = true, startedAt = nowMillis())
        val taskInProgressStuck = createTask(inProgress = true, startedAt = nowMillis().minusSeconds(60000))

        val new = randomDownloadTask(type = "item", pipeline = "api").toEvent()
        val samePriority = taskSamePriority.toEvent().copy(pipeline = "refresh")
        val higherPriority = taskHigherPriority.toEvent().copy(pipeline = "refresh", priority = 10)
        val lowerPriority = taskLowerPriority.toEvent().copy(priority = 10)
        val inProgress = taskInProgress.toEvent()
        val inProgressStuck = taskInProgressStuck.toEvent()

        downloadTaskService.update(
            "item",
            listOf(
                new,
                samePriority,
                higherPriority,
                lowerPriority,
                inProgress,
                inProgressStuck
            )
        )

        // New task successfully created
        assertThat(downloadTaskRepository.get(new.id)!!.inProgress).isFalse()

        // Existing task with lower priority should be updated
        assertThat(downloadTaskRepository.get(taskLowerPriority.id))
            .isEqualTo(taskLowerPriority.copy(version = 1, priority = 10))

        // Stuck task should be restored to be processed again
        assertThat(downloadTaskRepository.get(taskInProgressStuck.id))
            .isEqualTo(taskInProgressStuck.copy(version = 1, startedAt = null, inProgress = false))

        // Other should stay the same
        assertThat(downloadTaskRepository.get(taskSamePriority.id)).isEqualTo(taskSamePriority)
        assertThat(downloadTaskRepository.get(taskHigherPriority.id)).isEqualTo(taskHigherPriority)
        assertThat(downloadTaskRepository.get(taskInProgress.id)).isEqualTo(taskInProgress)
    }

    private suspend fun createTask(
        type: String = "item",
        pipeline: String = "api",
        priority: Int = 0,
        scheduledAt: Instant = nowMillis(),
        startedAt: Instant? = null,
        inProgress: Boolean = false
    ): DownloadTask {
        return downloadTaskRepository.save(
            randomDownloadTask(
                type = type,
                pipeline = pipeline,
                priority = priority,
                scheduledAt = scheduledAt,
                inProgress = inProgress,
                startedAt = startedAt
            )
        )
    }
}
