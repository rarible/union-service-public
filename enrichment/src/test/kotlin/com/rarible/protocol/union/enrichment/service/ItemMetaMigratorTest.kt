package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.LoadTaskId
import com.rarible.core.loader.internal.common.LoadTask
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.loader.cache.internal.CacheRepository
import com.rarible.loader.cache.internal.MongoCacheEntry
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ItemMetaMigratorTest {

    private val legacyRepository: CacheRepository = mockk()
    private val modernRepository: ItemMetaRepository = mockk()
    private val migrator = ItemMetaMigrator(legacyRepository, modernRepository)

    private lateinit var meta: UnionMeta
    private lateinit var itemId: ItemIdDto
    private lateinit var legacyEntry: MongoCacheEntry<UnionMeta>

    @BeforeEach
    fun beforeEach() {
        itemId = randomEthItemId()
        meta = randomUnionMeta()
        legacyEntry = MongoCacheEntry(itemId.fullId(), meta, nowMillis().minusSeconds(randomLong(1000)))

        clearMocks(legacyRepository, modernRepository)
        coEvery { modernRepository.save(any()) } answers { it.invocation.args[0] as DownloadEntry<UnionMeta> }
        coEvery { legacyRepository.get<UnionMeta>(ItemMetaDownloader.TYPE, itemId.fullId()) } returns legacyEntry
    }

    @Test
    fun `migrated - downloaded meta`() = runBlocking<Unit> {
        val status = LoadTask.Status.Loaded(nowMillis().minusSeconds(1), randomInt(), nowMillis())
        val task = createTask(itemId, status)

        coEvery { modernRepository.get(itemId.fullId()) } returns null

        migrator.migrate(task)

        coVerify(exactly = 1) {
            modernRepository.save(match {
                assertThat(it.id).isEqualTo(itemId.fullId())
                assertThat(it.status).isEqualTo(DownloadStatus.SUCCESS)
                assertThat(it.fails).isEqualTo(status.retryAttempts)
                assertThat(it.retries).isEqualTo(status.retryAttempts) // retries are not null
                assertThat(it.failedAt).isEqualTo(status.scheduledAt)
                assertThat(it.scheduledAt).isEqualTo(status.scheduledAt)
                assertThat(it.updatedAt).isEqualTo(legacyEntry.cachedAt)
                assertThat(it.succeedAt).isEqualTo(legacyEntry.cachedAt)
                assertThat(it.data).isEqualTo(meta)
                assertThat(it.downloads).isEqualTo(1)
                true
            })
        }
    }

    @Test
    fun `migrated - failed meta`() = runBlocking<Unit> {
        val status = LoadTask.Status.Failed(nowMillis().minusSeconds(1), randomInt(), nowMillis(), randomString())
        val task = createTask(itemId, status)

        coEvery { modernRepository.get(itemId.fullId()) } returns null

        migrator.migrate(task)

        coVerify(exactly = 1) {
            modernRepository.save(match {
                assertThat(it.id).isEqualTo(itemId.fullId())
                assertThat(it.status).isEqualTo(DownloadStatus.FAILED)
                assertThat(it.fails).isEqualTo(status.retryAttempts)
                assertThat(it.retries).isEqualTo(status.retryAttempts)
                assertThat(it.failedAt).isEqualTo(status.failedAt)
                assertThat(it.scheduledAt).isEqualTo(status.scheduledAt)
                assertThat(it.updatedAt).isEqualTo(status.failedAt)
                assertThat(it.succeedAt).isNull()
                assertThat(it.data).isNull()
                assertThat(it.downloads).isEqualTo(0)
                assertThat(it.errorMessage).isEqualTo(status.errorMessage)
                true
            })
        }
    }

    @Test
    fun `updated - downloaded meta`() = runBlocking<Unit> {
        val status = LoadTask.Status.Loaded(nowMillis().minusSeconds(1), randomInt(), nowMillis())
        val task = createTask(itemId, status)

        // Modern meta is outdated, should be migrated from legacy value
        val modernEntry = randomItemMetaDownloadEntry(
            status = DownloadStatus.SUCCESS,
            succeedAt = legacyEntry.cachedAt.minusSeconds(1)
        )

        coEvery { modernRepository.get(itemId.fullId()) } returns modernEntry

        migrator.migrate(task)

        coVerify(exactly = 1) {
            modernRepository.save(match {
                // Should be updated
                assertThat(it.data).isEqualTo(meta)
                // Should be kept
                assertThat(it.errorMessage).isEqualTo(modernEntry.errorMessage)
                true
            })
        }
    }

    @Test
    fun `updated - failed meta`() = runBlocking<Unit> {
        val status = LoadTask.Status.Failed(nowMillis().minusSeconds(1), randomInt(), nowMillis(), randomString())
        val task = createTask(itemId, status)

        // Modern meta is outdated, should be migrated from legacy value
        val modernEntry = randomItemMetaDownloadEntry(
            status = DownloadStatus.FAILED,
            failedAt = status.failedAt.minusSeconds(1)
        )

        coEvery { modernRepository.get(itemId.fullId()) } returns modernEntry

        migrator.migrate(task)

        coVerify(exactly = 1) {
            modernRepository.save(match {
                // Should be updated
                assertThat(it.errorMessage).isEqualTo(modernEntry.errorMessage)
                true
            })
        }
    }

    @Test
    fun `skipped - downloaded, but outdated`() = runBlocking<Unit> {
        val status = LoadTask.Status.Loaded(nowMillis().minusSeconds(1), randomInt(), nowMillis())
        val task = createTask(itemId, status)

        // Modern meta is outdated, nothing should happens
        val modernEntry = randomItemMetaDownloadEntry(
            status = DownloadStatus.SUCCESS,
            succeedAt = legacyEntry.cachedAt.plusSeconds(1)
        )

        coEvery { modernRepository.get(itemId.fullId()) } returns modernEntry

        migrator.migrate(task)

        coVerify(exactly = 0) { modernRepository.save(any()) }
    }

    @Test
    fun `skipped - failed, but modern's status is not failed`() = runBlocking<Unit> {
        val status = LoadTask.Status.Loaded(nowMillis().minusSeconds(1), randomInt(), nowMillis())
        val task = createTask(itemId, status)

        // Modern meta already has status SUCCEED, should be kept
        val modernEntry = randomItemMetaDownloadEntry(status = DownloadStatus.SUCCESS)

        coEvery { modernRepository.get(itemId.fullId()) } returns modernEntry

        migrator.migrate(task)

        coVerify(exactly = 0) { modernRepository.save(any()) }
    }

    @Test
    fun `skipped - retry or scheduled statuses`() = runBlocking<Unit> {
        val retry = LoadTask.Status.WaitsForRetry(nowMillis(), randomInt(), nowMillis(), nowMillis(), "", false)
        val scheduled = LoadTask.Status.Scheduled(nowMillis())


        migrator.migrate(createTask(itemId, retry))
        migrator.migrate(createTask(itemId, scheduled))

        coVerify(exactly = 0) { modernRepository.save(any()) }
        coVerify(exactly = 0) { modernRepository.get(any()) }
    }

    private fun createTask(itemId: ItemIdDto, stats: LoadTask.Status): LoadTask {
        return LoadTask(LoadTaskId(), "", itemId.fullId(), stats, 1)
    }
}


