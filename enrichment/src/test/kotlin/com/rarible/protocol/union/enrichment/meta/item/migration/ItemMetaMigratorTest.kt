package com.rarible.protocol.union.enrichment.meta.item.migration

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.LoadTaskId
import com.rarible.core.loader.internal.common.LoadTask
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.loader.cache.internal.CacheRepository
import com.rarible.loader.cache.internal.MongoCacheEntry
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
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
        coEvery { modernRepository.update(any(), any(), any()) } returns null // Doesn't matter for these tests
        coEvery { legacyRepository.get<UnionMeta>(ItemMetaDownloader.TYPE, itemId.fullId()) } returns legacyEntry
    }

    @Test
    fun `skipped - scheduled status`() = runBlocking<Unit> {
        val scheduled = LoadTask.Status.Scheduled(nowMillis())

        migrator.migrate(createTask(itemId, scheduled))

        coVerify(exactly = 0) { modernRepository.update(any(), any(), any()) }
        coVerify(exactly = 0) { modernRepository.get(any()) }
    }

    @Test
    fun `migrated - downloaded`() = runBlocking<Unit> {
        val status = LoadTask.Status.Loaded(nowMillis().minusSeconds(1), randomInt(), nowMillis())
        val task = createTask(itemId, status)

        coEvery { modernRepository.get(itemId.fullId()) } returns null
        coEvery { legacyRepository.get<UnionMeta>(ItemMetaDownloader.TYPE, itemId.fullId()) } returns legacyEntry

        migrator.migrate(task)

        coVerify(exactly = 1) { modernRepository.update(any(), any(), any()) }
    }

    @Test
    fun `migrated - failed`() = runBlocking<Unit> {
        val status = LoadTask.Status.Failed(nowMillis().minusSeconds(1), randomInt(), nowMillis(), randomString())
        val task = createTask(itemId, status)

        coEvery { modernRepository.get(itemId.fullId()) } returns null
        coEvery { legacyRepository.get<UnionMeta>(ItemMetaDownloader.TYPE, itemId.fullId()) } returns legacyEntry

        migrator.migrate(task)

        coVerify(exactly = 1) { modernRepository.update(any(), any(), any()) }
    }

    @Test
    fun `migrated - retry`() = runBlocking<Unit> {
        val status = LoadTask.Status.WaitsForRetry(
            nowMillis().minusSeconds(2),
            randomInt(),
            nowMillis().minusSeconds(1),
            nowMillis(),
            randomString(),
            true
        );
        val task = createTask(itemId, status)

        coEvery { modernRepository.get(itemId.fullId()) } returns null
        coEvery { legacyRepository.get<UnionMeta>(ItemMetaDownloader.TYPE, itemId.fullId()) } returns legacyEntry

        migrator.migrate(task)

        coVerify(exactly = 1) { modernRepository.update(any(), any(), any()) }
    }

    private fun createTask(itemId: ItemIdDto, stats: LoadTask.Status): LoadTask {
        return LoadTask(LoadTaskId(), "", itemId.fullId(), stats, 1)
    }
}


