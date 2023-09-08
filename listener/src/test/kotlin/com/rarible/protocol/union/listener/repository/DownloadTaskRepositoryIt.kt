package com.rarible.protocol.union.listener.repository

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.enrichment.download.DownloadTask
import com.rarible.protocol.union.enrichment.repository.DownloadTaskRepository
import com.rarible.protocol.union.enrichment.test.data.randomDownloadTask
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant

@IntegrationTest
class DownloadTaskRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var downloadTaskRepository: DownloadTaskRepository

    @Test
    fun `find for execution - ok`() = runBlocking<Unit> {
        val now = nowMillis()
        val api1 = createTask(priority = 20) // Highest priority - should be first
        val api2 = createTask(priority = 10, scheduledAt = now) // Scheduled later than next, should be third
        val api3 = createTask(priority = 10, scheduledAt = now.minusSeconds(1))
        val api5 = createTask(priority = 0) // Lowest priority

        // Should not be found
        createTask(priority = 20, inProgress = true)
        createTask(priority = 30, pipeline = "event")
        createTask(priority = 30, type = "collection")

        assertThat(downloadTaskRepository.findForExecution("item", "api"))
            .isEqualTo(listOf(api1, api3, api2, api5))
    }

    @Test
    fun `reactivate - ok`() = runBlocking<Unit> {
        val now = nowMillis()
        val api1 = createTask(startedAt = now.minusSeconds(10), inProgress = true)
        val api2 = createTask(startedAt = now.minusSeconds(60), inProgress = true)

        downloadTaskRepository.reactivateStuckTasks(Duration.ofSeconds(20))

        assertThat(downloadTaskRepository.findForExecution("item", "api"))
            .isEqualTo(listOf(api2.copy(inProgress = false, startedAt = null, version = 1)))
    }

    @Test
    fun `count in pipeline - ok`() = runBlocking<Unit> {
        val now = nowMillis()
        (1..5).map { createTask() }
        (1..2).map { createTask(type = "collection") }
        (1..1).map { createTask(pipeline = "refresh") }

        downloadTaskRepository.getTaskCountInPipeline("item", "api", 1)

        assertThat(downloadTaskRepository.getTaskCountInPipeline("item", "api", 10))
            .isEqualTo(5)

        assertThat(downloadTaskRepository.getTaskCountInPipeline("item", "api", 2))
            .isEqualTo(2)
    }

    private suspend fun createTask(
        type: String = "item",
        pipeline: String = "api",
        priority: Int = 0,
        scheduledAt: Instant = nowMillis(),
        startedAt: Instant = nowMillis(),
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
