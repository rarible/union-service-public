package com.rarible.protocol.union.meta.loader.executor

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.core.model.download.MetaProviderType
import com.rarible.protocol.union.core.model.download.PartialDownloadException
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.configuration.MetaTrimmingProperties
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadNotifier
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.meta.loader.test.AbstractIntegrationTest
import com.rarible.protocol.union.meta.loader.test.IntegrationTest
import com.rarible.protocol.union.meta.loader.test.data.randomFailedMetaEntry
import com.rarible.protocol.union.meta.loader.test.data.randomMetaEntry
import com.rarible.protocol.union.meta.loader.test.data.randomRetryMetaEntry
import com.rarible.protocol.union.meta.loader.test.data.randomTask
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.time.Instant

@IntegrationTest
class DownloadExecutorIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var repository: ItemMetaRepository

    @Autowired
    lateinit var trimmingProperties: MetaTrimmingProperties

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var metrics: DownloadExecutorMetrics

    val downloader: ItemMetaDownloader = mockk() { every { type } returns "Item" }
    val notifier: DownloadNotifier<UnionMeta> = mockk { coEvery { notify(any()) } returns Unit }
    val pool = DownloadPool(2, "item-meta-test")
    val maxRetries = 2

    lateinit var downloadExecutor: DownloadExecutor<UnionMeta>

    lateinit var now: Instant
    lateinit var itemId: ItemIdDto
    lateinit var fullItemId: String
    lateinit var meta: UnionMeta

    @BeforeEach
    fun beforeEach() {
        downloadExecutor = ItemDownloadExecutor(
            repository,
            downloader,
            notifier,
            pool,
            metrics,
            maxRetries,
        )
        now = nowMillis()
        itemId = randomEthItemId()
        fullItemId = itemId.fullId()
        meta = randomUnionMeta()
    }

    @Test
    fun `initial task - success`() = runBlocking<Unit> {
        val task = randomTask(fullItemId)
        createItem(itemId, null)
        val savedItem = itemRepository.get(ShortItemId(itemId))!!

        mockGetMeta(fullItemId, meta)

        downloadExecutor.execute(listOf(task))

        val saved = repository.get(fullItemId)!!

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
        verifyItemUpdated(itemId, savedItem)
    }

    @Test
    fun `initial task - success, trim`() = runBlocking<Unit> {
        val task = randomTask(fullItemId)
        createItem(itemId, null)
        itemRepository.get(ShortItemId(itemId))!!

        mockGetMeta(
            fullItemId,
            randomUnionMeta().copy(
                name = IntRange(0, trimmingProperties.nameLength * 2).joinToString { "x" },
                description = IntRange(0, trimmingProperties.descriptionLength * 2).joinToString { "x" }
            )
        )

        downloadExecutor.execute(listOf(task))

        val saved = repository.get(fullItemId)!!

        assertThat(saved.data?.name?.length)
            .isEqualTo(trimmingProperties.nameLength + trimmingProperties.suffix.length)

        assertThat(saved.data?.description?.length)
            .isEqualTo(trimmingProperties.descriptionLength + trimmingProperties.suffix.length)
    }

    @Test
    fun `initial task - failed`() = runBlocking<Unit> {
        val task = randomTask(fullItemId)
        createItem(itemId, null)
        val savedItem = itemRepository.get(ShortItemId(itemId))!!

        mockGetMetaFailed(fullItemId, "failed")

        downloadExecutor.execute(listOf(task))

        val saved = repository.get(fullItemId)!!

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
        verifyItemNotChanged(itemId, savedItem)
    }

    @Test
    fun `initial task - scheduled to succeeded`() = runBlocking<Unit> {
        val entry = randomRetryMetaEntry(fullItemId).copy(status = DownloadStatus.SCHEDULED)
        createItem(itemId, entry)

        val savedItem = itemRepository.get(ShortItemId(itemId))!!
        mockGetMeta(fullItemId, meta)

        downloadExecutor.execute(listOf(randomTask(fullItemId)))

        val saved = repository.get(fullItemId)!!
        assertThat(saved.data).isEqualTo(meta)
        assertThat(saved.status).isEqualTo(DownloadStatus.SUCCESS)
        assertThat(saved.downloads).isEqualTo(entry.downloads + 1)
        assertThat(saved.fails).isEqualTo(entry.fails)

        coVerify(exactly = 1) { notifier.notify(saved) }
        verifyItemUpdated(itemId, savedItem)
    }

    @Test
    fun `forced task - retry to succeeded`() = runBlocking<Unit> {
        val entry = randomFailedMetaEntry(fullItemId)
        createItem(itemId, entry)
        val savedItem = itemRepository.get(ShortItemId(itemId))!!
        mockGetMeta(fullItemId, meta)

        downloadExecutor.execute(listOf(randomTask(fullItemId)))

        val saved = repository.get(fullItemId)!!
        assertThat(saved.data).isEqualTo(meta)
        assertThat(saved.status).isEqualTo(DownloadStatus.SUCCESS)
        assertThat(saved.downloads).isEqualTo(entry.downloads + 1)
        assertThat(saved.fails).isEqualTo(entry.fails)

        coVerify(exactly = 1) { notifier.notify(saved) }
        verifyItemUpdated(itemId, savedItem)
    }

    @Test
    fun `forced task - failed to succeeded`() = runBlocking<Unit> {
        val entry = randomFailedMetaEntry(fullItemId)
        createItem(itemId, entry)
        val savedItem = itemRepository.get(ShortItemId(itemId))!!

        mockGetMeta(fullItemId, meta)

        downloadExecutor.execute(listOf(randomTask(fullItemId)))

        val saved = repository.get(fullItemId)!!
        assertThat(saved.data).isEqualTo(meta)
        assertThat(saved.status).isEqualTo(DownloadStatus.SUCCESS)
        assertThat(saved.downloads).isEqualTo(entry.downloads + 1)
        assertThat(saved.fails).isEqualTo(entry.fails)

        coVerify(exactly = 1) { notifier.notify(saved) }
        verifyItemUpdated(itemId, savedItem)
    }

    @Test
    fun `forced task - retry increased`() = runBlocking<Unit> {
        val entry = randomMetaEntry(fullItemId).copy(retries = 0, status = DownloadStatus.RETRY)
        createItem(itemId, entry)
        val savedItem = itemRepository.get(ShortItemId(itemId))!!
        mockGetMetaFailed(fullItemId, "error")

        downloadExecutor.execute(listOf(randomTask(fullItemId)))

        val saved = repository.get(fullItemId)!!
        assertThat(saved.data).isEqualTo(entry.data)
        assertThat(saved.status).isEqualTo(DownloadStatus.RETRY)
        assertThat(saved.downloads).isEqualTo(entry.downloads)
        assertThat(saved.fails).isEqualTo(entry.fails + 1)

        coVerify(exactly = 0) { notifier.notify(any()) }
        verifyItemNotChanged(itemId, savedItem)
    }

    @Test
    fun `forced task - retries exhausted`() = runBlocking<Unit> {
        val entry = randomMetaEntry(fullItemId).copy(
            retries = maxRetries,
            status = DownloadStatus.RETRY,
            data = null
        )
        createItem(itemId, entry)
        val savedItem = itemRepository.get(ShortItemId(itemId))!!
        mockGetMetaFailed(fullItemId, "error")

        downloadExecutor.execute(listOf(randomTask(fullItemId)))

        val saved = repository.get(fullItemId)!!
        assertThat(saved.data).isEqualTo(entry.data)
        assertThat(saved.status).isEqualTo(DownloadStatus.FAILED)
        assertThat(saved.downloads).isEqualTo(entry.downloads)
        assertThat(saved.fails).isEqualTo(entry.fails + 1)

        coVerify(exactly = 0) { notifier.notify(any()) }
        verifyItemNotChanged(itemId, savedItem)
    }

    @Test
    fun `forced task - sill fails`() = runBlocking<Unit> {
        val entry = randomMetaEntry(fullItemId).copy(retries = maxRetries + 1, status = DownloadStatus.FAILED)
        createItem(itemId, entry)
        mockGetMetaFailed(fullItemId, "error")

        downloadExecutor.execute(listOf(randomTask(fullItemId)))

        val saved = repository.get(fullItemId)!!
        assertThat(saved.data).isEqualTo(entry.data)
        assertThat(saved.status).isEqualTo(DownloadStatus.FAILED)
        assertThat(saved.downloads).isEqualTo(entry.downloads)
        assertThat(saved.fails).isEqualTo(entry.fails + 1)

        coVerify(exactly = 0) { notifier.notify(any()) }
        val savedItem = itemRepository.get(ShortItemId(itemId))!!
        verifyItemNotChanged(itemId, savedItem)
    }

    @Test
    fun `forced task - debounce`() = runBlocking {
        createItem(itemId, randomMetaEntry(fullItemId))
        downloadExecutor.execute(listOf(randomTask(fullItemId).copy(scheduledAt = Instant.now().minusSeconds(1))))

        coVerify(exactly = 0) { downloader.download(any()) }
    }

    @Test
    fun `forced task - partial retry increased`() = runBlocking<Unit> {
        val entry = randomMetaEntry(fullItemId).copy(
            retries = 0,
            status = DownloadStatus.RETRY_PARTIAL,
            failedProviders = listOf(MetaProviderType.SIMPLE_HASH),
        )
        createItem(itemId, entry)
        val savedItem = itemRepository.get(ShortItemId(itemId))!!
        mockGetMetaFailed(fullItemId, "error")

        downloadExecutor.execute(listOf(randomTask(fullItemId)))

        val saved = repository.get(fullItemId)!!
        assertThat(saved.data).isEqualTo(entry.data)
        assertThat(saved.status).isEqualTo(DownloadStatus.RETRY_PARTIAL)
        assertThat(saved.downloads).isEqualTo(entry.downloads)
        assertThat(saved.fails).isEqualTo(entry.fails + 1)
        assertThat(saved.failedProviders).isEqualTo(entry.failedProviders)

        coVerify(exactly = 0) { notifier.notify(any()) }
        verifyItemNotChanged(itemId, savedItem)
    }

    @Test
    fun `initial task - partially failed`() = runBlocking<Unit> {
        val task = randomTask(fullItemId)
        createItem(itemId, null)

        val partialMeta = randomUnionMeta()
        coEvery { downloader.download(fullItemId) } throws PartialDownloadException(
            failedProviders = listOf(MetaProviderType.SIMPLE_HASH),
            data = partialMeta,
        )

        downloadExecutor.execute(listOf(task))

        val saved = repository.get(fullItemId)!!

        assertThat(saved.status).isEqualTo(DownloadStatus.RETRY_PARTIAL)
        assertThat(saved.failedProviders).containsExactly(MetaProviderType.SIMPLE_HASH)
        assertThat(saved.fails).isEqualTo(1)
        assertThat(saved.retries).isEqualTo(0)
        assertThat(saved.downloads).isEqualTo(0)
        assertThat(saved.succeedAt).isNull()
        assertThat(saved.failedAt).isAfterOrEqualTo(now)
        assertThat(saved.updatedAt).isEqualTo(saved.failedAt)
        assertThat(saved.scheduledAt).isEqualTo(task.scheduledAt)
        assertThat(saved.errorMessage).isEqualTo("Failed to download meta from providers: [SIMPLE_HASH]")
        assertThat(saved.data).isEqualTo(partialMeta)

        coVerify(exactly = 1) { notifier.notify(saved) }
    }

    @Test
    fun `partial retry - failed`() = runBlocking<Unit> {
        val task = randomTask(fullItemId)
        val partialMeta = randomUnionMeta()
        createItem(
            itemId, randomMetaEntry(itemId = fullItemId, meta = partialMeta).copy(
                status = DownloadStatus.RETRY_PARTIAL,
                failedProviders = listOf(MetaProviderType.SIMPLE_HASH),
                retries = 0,
                fails = 1,
                downloads = 0,
                succeedAt = null,
            )
        )
        val savedItem = itemRepository.get(ShortItemId(itemId))!!

        coEvery { downloader.download(fullItemId) } throws DownloadException("failed")

        downloadExecutor.execute(listOf(task))

        val saved = repository.get(fullItemId)!!

        assertThat(saved.status).isEqualTo(DownloadStatus.RETRY_PARTIAL)
        assertThat(saved.failedProviders).containsExactly(MetaProviderType.SIMPLE_HASH)
        assertThat(saved.fails).isEqualTo(2)
        assertThat(saved.retries).isEqualTo(0)
        assertThat(saved.downloads).isEqualTo(0)
        assertThat(saved.succeedAt).isNull()
        assertThat(saved.failedAt).isAfterOrEqualTo(now)
        assertThat(saved.updatedAt).isEqualTo(saved.failedAt)
        assertThat(saved.errorMessage).isEqualTo("failed")
        assertThat(saved.data).isEqualTo(partialMeta)

        coVerify(exactly = 0) { notifier.notify(any()) }
        verifyItemNotChanged(itemId, savedItem)
    }

    private suspend fun createItem(itemId: ItemIdDto, metaEntry: DownloadEntry<UnionMeta>?): ShortItem =
        itemRepository.save(
            ShortItem(
                itemId = itemId.value,
                blockchain = itemId.blockchain,
                bestBidOrder = null,
                bestBidOrders = emptyMap(),
                bestSellOrder = null,
                bestSellOrders = emptyMap(),
                lastSale = null,
                lastUpdatedAt = Instant.ofEpochMilli(0),
                totalStock = BigInteger.ZERO,
                metaEntry = metaEntry
            )
        )

    private suspend fun verifyItemUpdated(itemId: ItemIdDto, savedItem: ShortItem) {
        val updatedItem = itemRepository.get(ShortItemId(itemId))!!
        assertThat(updatedItem.version!! - savedItem.version!!).isEqualTo(1)
        assertThat(updatedItem.lastUpdatedAt.toEpochMilli() - nowMillis().toEpochMilli()).isLessThan(100)
    }

    private suspend fun verifyItemNotChanged(itemId: ItemIdDto, savedItem: ShortItem) {
        val updatedItem = itemRepository.get(ShortItemId(itemId))!!
        assertThat(updatedItem.lastUpdatedAt).isEqualTo(savedItem.lastUpdatedAt)
    }

    private fun mockGetMeta(itemId: String, meta: UnionMeta) {
        coEvery { downloader.download(itemId) } returns meta
    }

    private fun mockGetMetaFailed(itemId: String, message: String) {
        coEvery { downloader.download(itemId) } throws IllegalArgumentException(message)
    }
}