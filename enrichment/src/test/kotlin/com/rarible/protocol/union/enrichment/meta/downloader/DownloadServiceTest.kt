package com.rarible.protocol.union.enrichment.meta.downloader

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaNotifier
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaTaskPublisher
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.repository.RawMetaCacheRepository
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("UNCHECKED_CAST")
class DownloadServiceTest {

    private val repository: ItemMetaRepository = mockk {
        coEvery { update(any(), any()) } answers {
            (it.invocation.args[1] as (current: DownloadEntry<UnionMeta>?) -> DownloadEntry<UnionMeta>)(null)
        }
    }

    private val rawMetaCacheRepository: RawMetaCacheRepository = mockk()

    private val publisher: ItemMetaTaskPublisher = mockk { coEvery { publish(any()) } returns Unit }
    private val notifier: ItemMetaNotifier = mockk { coEvery { notify(any()) } returns Unit }

    private val metrics: DownloadMetrics = DownloadMetrics(LoggingMeterRegistry())

    private val downloader: ItemMetaDownloader = mockk() { every { type } returns "Item" }

    private val downloadService = ItemMetaService(
        rawMetaCacheRepository,
        repository,
        publisher,
        downloader,
        notifier,
        metrics
    )

    private lateinit var itemId: ItemIdDto
    private val pipeline = ItemMetaPipeline.REFRESH

    @BeforeEach
    fun beforeEach() {
        itemId = randomEthItemId()
    }

    @Test
    fun `download - ok`() = runBlocking<Unit> {
        val meta = randomUnionMeta()
        coEvery { downloader.download(itemId.fullId()) } returns meta

        val result = downloadService.download(itemId, pipeline, false)

        assertThat(result).isEqualTo(meta)

        // Updated and notified
        coVerify(exactly = 1) { repository.update(eq(itemId.fullId()), any()) }
        coVerify(exactly = 1) {
            notifier.notify(
                match {
                    assertThat(it.data).isEqualTo(meta)
                    assertThat(it.id).isEqualTo(itemId.fullId())
                    assertThat(it.status).isEqualTo(DownloadStatus.SUCCESS)
                    true
                }
            )
        }
    }

    @Test
    fun `download - failed, entry exists`() = runBlocking<Unit> {
        val current = randomItemMetaDownloadEntry(itemId.fullId())

        coEvery { downloader.download(itemId.fullId()) } throws DownloadException("Oops")
        coEvery { repository.get(itemId.fullId()) } returns current

        val result = downloadService.download(itemId, pipeline, false)

        assertThat(result).isNull()

        // Updated in DB, but no notifications sent since entry already exists
        coVerify(exactly = 1) { repository.update(eq(itemId.fullId()), any()) }
        coVerify(exactly = 0) { notifier.notify(any()) }
        coVerify(exactly = 0) { publisher.publish(any()) }
    }

    @Test
    fun `download - failed, entry doesn't exists`() = runBlocking<Unit> {
        coEvery { downloader.download(itemId.fullId()) } throws DownloadException("Oops")
        coEvery { repository.get(itemId.fullId()) } returns null

        val result = downloadService.download(itemId, pipeline, false)

        assertThat(result).isNull()

        // Nothing to update, just scheduled
        coVerify(exactly = 0) { repository.update(eq(itemId.fullId()), any()) }
        coVerify(exactly = 0) { notifier.notify(any()) }
        coVerify(exactly = 1) { publisher.publish(match { it.size == 1 && it[0].id == itemId.fullId() }) }
    }

    @Test
    fun `save - ok`() = runBlocking<Unit> {
        val meta = randomUnionMeta()

        downloadService.save(itemId, meta)

        // Updated and notified
        coVerify(exactly = 1) { repository.update(eq(itemId.fullId()), any()) }
        coVerify(exactly = 1) {
            notifier.notify(
                match {
                    assertThat(it.data).isEqualTo(meta)
                    assertThat(it.id).isEqualTo(itemId.fullId())
                    assertThat(it.status).isEqualTo(DownloadStatus.SUCCESS)
                    true
                }
            )
        }
    }

    @Test
    fun `schedule - ok`() = runBlocking<Unit> {
        downloadService.schedule(itemId, pipeline, false)

        // Nothing to update, just scheduled
        coVerify(exactly = 0) { repository.update(eq(itemId.fullId()), any()) }
        coVerify(exactly = 0) { notifier.notify(any()) }
        coVerify(exactly = 1) { publisher.publish(match { it.size == 1 && it[0].id == itemId.fullId() }) }
    }
}
