package com.rarible.protocol.union.meta.loader.executor

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadNotifier
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.meta.loader.test.AbstractIntegrationTest
import com.rarible.protocol.union.meta.loader.test.IntegrationTest
import com.rarible.protocol.union.meta.loader.test.data.randomFailedMetaEntry
import com.rarible.protocol.union.meta.loader.test.data.randomRetryMetaEntry
import com.rarible.protocol.union.meta.loader.test.data.randomTask
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class DownloadExecutorIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var debouncer: DownloadDebouncer

    @Autowired
    lateinit var repository: ItemMetaRepository

    val downloader: ItemMetaDownloader = mockk()
    val notifier: DownloadNotifier<UnionMeta> = mockk { coEvery { notify(any()) } returns Unit }
    val pool = DownloadPool(2, "item-meta-test")
    val maxRetries = 2

    lateinit var downloadExecutor: DownloadExecutor<UnionMeta>

    @BeforeEach
    fun beforeEach() {
        downloadExecutor = DownloadExecutor(
            repository,
            downloader,
            debouncer,
            notifier,
            pool,
            maxRetries
        )
    }

    @Test
    fun `initial task - success`() = runBlocking<Unit> {
        val now = nowMillis()
        val itemId = randomEthItemId().fullId()
        val meta = randomUnionMeta()
        val task = randomTask(itemId)

        mockGetMeta(itemId, meta)

        downloadExecutor.execute(listOf(task))

        val saved = repository.get(itemId)!!

        assertThat(saved.data).isEqualTo(meta)
        assertThat(saved.status).isEqualTo(DownloadStatus.SUCCESS)
        assertThat(saved.fails).isEqualTo(0)
        assertThat(saved.retries).isEqualTo(0)
        assertThat(saved.downloads).isEqualTo(1)
        assertThat(saved.succeedAt).isAfterOrEqualTo(now)
        assertThat(saved.failedAt).isNull()
        assertThat(saved.updatedAt).isEqualTo(saved.succeedAt)
        assertThat(saved.scheduledAt).isEqualTo(task.scheduledAt)
        assertThat(saved.errorMessage).isNull()

        coVerify(exactly = 1) { notifier.notify(saved) }
    }

    @Test
    fun `initial task - failed`() = runBlocking<Unit> {
        val now = nowMillis()
        val itemId = randomEthItemId().fullId()
        val task = randomTask(itemId)

        mockGetMetaFailed(itemId, "failed")

        downloadExecutor.execute(listOf(task))

        val saved = repository.get(itemId)!!

        assertThat(saved.data).isNull()
        assertThat(saved.status).isEqualTo(DownloadStatus.RETRY)
        assertThat(saved.fails).isEqualTo(1)
        assertThat(saved.retries).isEqualTo(0)
        assertThat(saved.downloads).isEqualTo(0)
        assertThat(saved.succeedAt).isNull()
        assertThat(saved.failedAt).isAfterOrEqualTo(now)
        assertThat(saved.updatedAt).isEqualTo(saved.failedAt)
        assertThat(saved.scheduledAt).isEqualTo(task.scheduledAt)
        assertThat(saved.errorMessage).isEqualTo("failed")

        coVerify(exactly = 0) { notifier.notify(any()) }
    }

    @Test
    fun `initial task - scheduled to succeeded`() = runBlocking<Unit> {
        val itemId = randomEthItemId().fullId()
        val entry = repository.save(randomRetryMetaEntry(itemId).copy(status = DownloadStatus.SCHEDULED))
        val meta = randomUnionMeta()
        mockGetMeta(itemId, meta)

        downloadExecutor.execute(listOf(randomTask(itemId)))

        val saved = repository.get(itemId)!!
        assertThat(saved.data).isEqualTo(meta)
        assertThat(saved.status).isEqualTo(DownloadStatus.SUCCESS)
        assertThat(saved.downloads).isEqualTo(entry.downloads + 1)
        assertThat(saved.fails).isEqualTo(entry.fails)

        coVerify(exactly = 1) { notifier.notify(saved) }
    }

    @Test
    fun `forced task - retry to succeeded`() = runBlocking<Unit> {
        val itemId = randomEthItemId().fullId()
        val entry = repository.save(randomRetryMetaEntry(itemId))
        val meta = randomUnionMeta()
        mockGetMeta(itemId, meta)

        downloadExecutor.execute(listOf(randomTask(itemId)))

        val saved = repository.get(itemId)!!
        assertThat(saved.data).isEqualTo(meta)
        assertThat(saved.status).isEqualTo(DownloadStatus.SUCCESS)
        assertThat(saved.downloads).isEqualTo(entry.downloads + 1)
        assertThat(saved.fails).isEqualTo(entry.fails)

        coVerify(exactly = 1) { notifier.notify(saved) }
    }

    @Test
    fun `forced task - failed to succeeded`() = runBlocking<Unit> {
        val itemId = randomEthItemId().fullId()
        val entry = repository.save(randomFailedMetaEntry(itemId))
        val meta = randomUnionMeta()
        mockGetMeta(itemId, meta)

        downloadExecutor.execute(listOf(randomTask(itemId)))

        val saved = repository.get(itemId)!!
        assertThat(saved.data).isEqualTo(meta)
        assertThat(saved.status).isEqualTo(DownloadStatus.SUCCESS)
        assertThat(saved.downloads).isEqualTo(entry.downloads + 1)
        assertThat(saved.fails).isEqualTo(entry.fails)

        coVerify(exactly = 1) { notifier.notify(saved) }
    }

    @Test
    fun `forced task - retry increased`() = runBlocking<Unit> {
        val itemId = randomEthItemId().fullId()
        val entry = repository.save(randomRetryMetaEntry(itemId).copy(retries = 0))
        mockGetMetaFailed(itemId, "error")

        downloadExecutor.execute(listOf(randomTask(itemId)))

        val saved = repository.get(itemId)!!
        assertThat(saved.data).isEqualTo(entry.data)
        assertThat(saved.status).isEqualTo(DownloadStatus.RETRY)
        assertThat(saved.downloads).isEqualTo(entry.downloads)
        assertThat(saved.fails).isEqualTo(entry.fails + 1)

        coVerify(exactly = 0) { notifier.notify(any()) }
    }

    @Test
    fun `forced task - retries exhausted`() = runBlocking<Unit> {
        val itemId = randomEthItemId().fullId()
        val entry = repository.save(randomRetryMetaEntry(itemId).copy(retries = maxRetries))
        mockGetMetaFailed(itemId, "error")

        downloadExecutor.execute(listOf(randomTask(itemId)))

        val saved = repository.get(itemId)!!
        assertThat(saved.data).isEqualTo(entry.data)
        assertThat(saved.status).isEqualTo(DownloadStatus.FAILED)
        assertThat(saved.downloads).isEqualTo(entry.downloads)
        assertThat(saved.fails).isEqualTo(entry.fails + 1)

        coVerify(exactly = 0) { notifier.notify(any()) }
    }

    @Test
    fun `forced task - sill fails`() = runBlocking<Unit> {
        val itemId = randomEthItemId().fullId()
        val entry = repository.save(randomFailedMetaEntry(itemId).copy(retries = maxRetries + 1))
        mockGetMetaFailed(itemId, "error")

        downloadExecutor.execute(listOf(randomTask(itemId)))

        val saved = repository.get(itemId)!!
        assertThat(saved.data).isEqualTo(entry.data)
        assertThat(saved.status).isEqualTo(DownloadStatus.FAILED)
        assertThat(saved.downloads).isEqualTo(entry.downloads)
        assertThat(saved.fails).isEqualTo(entry.fails + 1)

        coVerify(exactly = 0) { notifier.notify(any()) }
    }

    // TODO add test with debouncing

    private fun mockGetMeta(itemId: String, meta: UnionMeta) {
        coEvery { downloader.download(itemId) } returns meta
    }

    private fun mockGetMetaFailed(itemId: String, message: String) {
        coEvery { downloader.download(itemId) } throws IllegalArgumentException(message)
    }

}