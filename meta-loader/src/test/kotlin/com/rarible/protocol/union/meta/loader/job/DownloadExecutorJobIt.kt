package com.rarible.protocol.union.meta.loader.job

import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.repository.DownloadTaskRepository
import com.rarible.protocol.union.enrichment.repository.LockRepository
import com.rarible.protocol.union.enrichment.service.DownloadTaskService
import com.rarible.protocol.union.enrichment.test.data.randomDownloadTask
import com.rarible.protocol.union.meta.loader.executor.ItemDownloadExecutor
import com.rarible.protocol.union.meta.loader.test.AbstractIntegrationTest
import com.rarible.protocol.union.meta.loader.test.IntegrationTest
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("UNCHECKED_CAST", "DeferredResultUnused")
@IntegrationTest
class DownloadExecutorJobIt : AbstractIntegrationTest() {

    val executor: ItemDownloadExecutor = mockk()

    @Autowired
    lateinit var downloadTaskService: DownloadTaskService

    @Autowired
    lateinit var downloadTaskRepository: DownloadTaskRepository

    @Autowired
    lateinit var lockRepository: LockRepository

    lateinit var downloadExecutorJob: DownloadExecutorJob

    @BeforeEach
    fun beforeEach() {
        clearMocks(executor)

        every { executor.type } returns "item"
        coEvery { executor.submit(any(), any()) } coAnswers {
            val event = it.invocation.args[0] as DownloadTaskEvent
            (it.invocation.args[1] as (suspend (e: DownloadTaskEvent) -> Unit))(event)
            CompletableDeferred(Unit)
        }

        downloadExecutorJob = DownloadExecutorJob(
            meterRegistry = mockk(),
            workerName = "test_worker",
            executor = executor,
            downloadTaskService = downloadTaskService,
            lockRepository = lockRepository,
            pipeline = "test",
            poolSize = 4
        )
    }

    @Test
    fun `handle - ok`() = runBlocking<Unit> {
        val toBeHandled = (1..16).map {
            downloadTaskRepository.save(randomDownloadTask(pipeline = "test")).id
        }

        val skip1 = downloadTaskRepository.save(randomDownloadTask(pipeline = "test", inProgress = true))
        val skip2 = downloadTaskRepository.save(randomDownloadTask(pipeline = "test2"))
        val skip3 = downloadTaskRepository.save(randomDownloadTask(type = "collection", pipeline = "test"))

        downloadExecutorJob.handle()

        assertThat(downloadTaskRepository.getByIds(toBeHandled)).isEmpty()
        assertThat(downloadTaskRepository.get(skip1.id)).isEqualTo(skip1)
        assertThat(downloadTaskRepository.get(skip2.id)).isEqualTo(skip2)
        assertThat(downloadTaskRepository.get(skip3.id)).isEqualTo(skip3)
        assertThat(lockRepository.get("meta_download_executor_item_test")!!.acquired).isFalse()

        coVerify(exactly = 16) { executor.submit(any(), any()) }
        toBeHandled.forEach { id -> coVerify(exactly = 1) { executor.submit(match { it.id == id }, any()) } }
    }
}
