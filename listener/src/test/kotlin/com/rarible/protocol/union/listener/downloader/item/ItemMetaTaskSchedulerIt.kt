package com.rarible.protocol.union.listener.downloader.item

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.downloader.DownloadSchedulerMetrics
import com.rarible.protocol.union.listener.downloader.ItemMetaTaskRouter
import com.rarible.protocol.union.listener.downloader.ItemMetaTaskScheduler
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class ItemMetaTaskSchedulerIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var repository: ItemMetaRepository

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var metrics: DownloadSchedulerMetrics

    @Autowired
    lateinit var blockchainRouter: BlockchainRouter<ItemService>

    private val router: ItemMetaTaskRouter = mockk()

    lateinit var scheduler: ItemMetaTaskScheduler

    @BeforeEach
    fun beforeEach() {
        clearMocks(router)
        coEvery { router.send(any(), any()) } returns Unit
        scheduler = ItemMetaTaskScheduler(router, repository, metrics, blockchainRouter)
    }

    @Test
    fun `not forced task - entry doesn't exist`() = runBlocking {
        val id = randomEthItemId().fullId()
        val task = DownloadTask(id, "api", false, nowMillis())

        scheduler.schedule(task)

        val entry = repository.get(id)!!

        // Entry should be created with default state, task should be routed to the executor
        assertThat(entry.status).isEqualTo(DownloadStatus.SCHEDULED)
        assertThat(entry.scheduledAt).isEqualTo(task.scheduledAt)
        assertThat(entry.data).isNull()

        coVerify(exactly = 1) { router.send(listOf(task), task.pipeline) }
    }

    @Test
    fun `not forced task - entry exists`() = runBlocking {
        val exist = save(randomItemMetaDownloadEntry())
        val task = DownloadTask(exist.id, "api", false, nowMillis())

        scheduler.schedule(task)

        val entry = repository.get(exist.id)!!

        // Entry not changed, nothing sent
        assertThat(entry).isEqualTo(exist)

        coVerify(exactly = 0) { router.send(any(), task.pipeline) }
    }

    @Test
    fun `not forced task - several tasks for same non-existing entry`() = runBlocking {
        val id = randomEthItemId().fullId()
        val task1 = DownloadTask(id, "api", false, nowMillis())
        val task2 = DownloadTask(id, "api", false, nowMillis())
        val task3 = DownloadTask(id, "listener", false, nowMillis())

        scheduler.schedule(listOf(task1, task2, task3))

        val entry = repository.get(id)
        assertThat(entry).isNotNull

        // Two batches sent to different pipelines
        coVerify(exactly = 1) { router.send(listOf(task1, task2), task1.pipeline) }
        coVerify(exactly = 1) { router.send(listOf(task3), task3.pipeline) }
    }

    @Test
    fun `forced task - entry exists`() = runBlocking {
        val exist = save(randomItemMetaDownloadEntry())
        val task = DownloadTask(exist.id, "api", true, nowMillis())

        scheduler.schedule(task)

        val entry = repository.get(exist.id)!!

        // Entry not changed, but task sent
        assertThat(entry).isEqualTo(exist)

        coVerify(exactly = 1) { router.send(listOf(task), task.pipeline) }
    }

    @Test
    fun `mixed tasks - entry exists`() = runBlocking {
        val exist = save(randomItemMetaDownloadEntry())
        val task1 = DownloadTask(exist.id, "api", true, nowMillis())
        val task2 = DownloadTask(exist.id, "listener", false, nowMillis())

        scheduler.schedule(listOf(task1, task2))

        // Only forced task routed
        coVerify(exactly = 1) { router.send(listOf(task1), task1.pipeline) }
        coVerify(exactly = 0) { router.send(listOf(task2), task2.pipeline) }
    }

    private suspend fun save(entry: DownloadEntry<UnionMeta>): DownloadEntry<UnionMeta> {
        itemRepository.save(ShortItem.empty(ShortItemId.of(entry.id)).withMeta(entry))
        return entry
    }
}