package com.rarible.protocol.union.listener.downloader.item

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.enrichment.download.DownloadEntry
import com.rarible.protocol.union.enrichment.download.DownloadStatus
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.DownloadTaskRepository
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.downloader.ItemMetaTaskScheduler
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class ItemMetaTaskSchedulerIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var repository: ItemMetaRepository

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var downloadTaskRepository: DownloadTaskRepository

    @Autowired
    lateinit var scheduler: ItemMetaTaskScheduler

    @Test
    fun `not forced task - entry doesn't exist`() = runBlocking<Unit> {
        val id = randomEthItemId().fullId()
        val task = DownloadTaskEvent(
            id = id,
            pipeline = "api",
            force = false,
            scheduledAt = nowMillis()
        )

        scheduler.schedule(task)

        val entry = repository.get(id)!!

        // Entry should be created with default state, task should be created
        assertThat(entry.status).isEqualTo(DownloadStatus.SCHEDULED)
        assertThat(entry.scheduledAt).isEqualTo(task.scheduledAt)
        assertThat(entry.data).isNull()

        val downloadTask = downloadTaskRepository.get(id)!!
        assertThat(downloadTask.scheduledAt).isEqualTo(task.scheduledAt)
        assertThat(downloadTask.pipeline).isEqualTo(task.pipeline)
    }

    @Test
    fun `not forced task - entry exists`() = runBlocking {
        val exist = save(randomItemMetaDownloadEntry())
        val task = DownloadTaskEvent(
            id = exist.id,
            pipeline = "api",
            force = false,
            scheduledAt = nowMillis()
        )

        scheduler.schedule(task)

        val entry = repository.get(exist.id)!!

        // Entry not changed, nothing sent
        assertThat(entry).isEqualTo(exist)

        assertThat(downloadTaskRepository.get(task.id)).isNull()
    }

    @Test
    fun `not forced task - several tasks for same non-existing entry`() = runBlocking<Unit> {
        val id = randomEthItemId().fullId()
        val task1 = DownloadTaskEvent(
            id = id,
            pipeline = "api",
            force = false,
            scheduledAt = nowMillis()
        )
        val task2 = DownloadTaskEvent(
            id = id,
            pipeline = "api",
            force = false,
            priority = 100,
            scheduledAt = nowMillis()
        )
        val task3 = DownloadTaskEvent(
            id = id,
            pipeline = "listener",
            force = false,
            scheduledAt = nowMillis()
        )

        scheduler.schedule(listOf(task1, task2, task3))

        val entry = repository.get(id)
        assertThat(entry).isNotNull

        val downloadTask = downloadTaskRepository.get(id)!!
        assertThat(downloadTask.scheduledAt).isEqualTo(task2.scheduledAt)
        assertThat(downloadTask.pipeline).isEqualTo(task2.pipeline)
        assertThat(downloadTask.priority).isEqualTo(task2.priority)
    }

    @Test
    fun `forced task - entry exists`() = runBlocking<Unit> {
        val exist = save(randomItemMetaDownloadEntry())
        val task = DownloadTaskEvent(
            id = exist.id,
            pipeline = "api",
            force = true,
            scheduledAt = nowMillis()
        )

        scheduler.schedule(task)

        val entry = repository.get(exist.id)!!

        // Entry not changed, but task sent
        assertThat(entry).isEqualTo(exist)

        val downloadTask = downloadTaskRepository.get(task.id)!!
        assertThat(downloadTask.scheduledAt).isEqualTo(task.scheduledAt)
        assertThat(downloadTask.pipeline).isEqualTo(task.pipeline)
    }

    @Test
    fun `mixed tasks - entry exists`() = runBlocking<Unit> {
        val exist = save(randomItemMetaDownloadEntry())
        val task1 = DownloadTaskEvent(
            id = exist.id,
            pipeline = "api",
            force = true,
            scheduledAt = nowMillis()
        )
        val task2 = DownloadTaskEvent(
            id = exist.id,
            pipeline = "listener",
            force = false,
            scheduledAt = nowMillis()
        )

        scheduler.schedule(listOf(task1, task2))

        // Only forced task created
        val downloadTask = downloadTaskRepository.get(task1.id)!!
        assertThat(downloadTask.scheduledAt).isEqualTo(task1.scheduledAt)
        assertThat(downloadTask.pipeline).isEqualTo(task1.pipeline)
    }

    private suspend fun save(entry: DownloadEntry<UnionMeta>): DownloadEntry<UnionMeta> {
        itemRepository.save(ShortItem.empty(ShortItemId.of(entry.id)).withMeta(entry))
        return entry
    }
}
