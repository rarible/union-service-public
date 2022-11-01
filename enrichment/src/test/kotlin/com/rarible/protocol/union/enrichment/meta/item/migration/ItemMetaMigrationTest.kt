package com.rarible.protocol.union.enrichment.meta.item.migration

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.LoadTaskId
import com.rarible.core.loader.internal.common.LoadTask
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.loader.cache.internal.MongoCacheEntry
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ItemMetaMigrationTest {

    private lateinit var meta: UnionMeta
    private lateinit var itemId: ItemIdDto
    private lateinit var legacyEntry: MongoCacheEntry<UnionMeta>

    @BeforeEach
    fun beforeEach() {
        itemId = randomEthItemId()
        meta = randomUnionMeta()
        legacyEntry = MongoCacheEntry(itemId.fullId(), meta, nowMillis().minusSeconds(randomLong(1000)))
    }

    @Test
    fun `migrated - downloaded`() {
        val status = LoadTask.Status.Loaded(nowMillis().minusSeconds(1), randomInt(), nowMillis())
        val task = createTask(itemId, status)

        val migration = DownloadedMetaMigration(task, null, legacyEntry)
        val migrated = migration.update(null)

        assertThat(migrated.id).isEqualTo(itemId.fullId())
        assertThat(migrated.status).isEqualTo(DownloadStatus.SUCCESS)
        assertThat(migrated.fails).isEqualTo(status.retryAttempts)
        assertThat(migrated.retries).isEqualTo(status.retryAttempts) // retries are not null
        assertThat(migrated.failedAt).isEqualTo(status.scheduledAt)
        assertThat(migrated.scheduledAt).isEqualTo(status.scheduledAt)
        assertThat(migrated.updatedAt).isEqualTo(legacyEntry.cachedAt)
        assertThat(migrated.succeedAt).isEqualTo(legacyEntry.cachedAt)
        assertThat(migrated.data).isEqualTo(meta)
        assertThat(migrated.downloads).isEqualTo(1)
    }

    @Test
    fun `migrated - failed`() {
        val status = LoadTask.Status.Failed(nowMillis().minusSeconds(1), randomInt(), nowMillis(), randomString())
        val task = createTask(itemId, status)

        val migration = FailedMetaMigration(task, null)
        val migrated = migration.update(null)

        assertThat(migrated.id).isEqualTo(itemId.fullId())
        assertThat(migrated.status).isEqualTo(DownloadStatus.FAILED)
        assertThat(migrated.fails).isEqualTo(status.retryAttempts)
        assertThat(migrated.retries).isEqualTo(status.retryAttempts)
        assertThat(migrated.failedAt).isEqualTo(status.failedAt)
        assertThat(migrated.scheduledAt).isEqualTo(status.scheduledAt)
        assertThat(migrated.updatedAt).isEqualTo(status.failedAt)
        assertThat(migrated.succeedAt).isNull()
        assertThat(migrated.data).isNull()
        assertThat(migrated.downloads).isEqualTo(0)
        assertThat(migrated.errorMessage).isEqualTo(status.errorMessage)
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

        val migration = DownloadedMetaMigration(task, modernEntry, legacyEntry)
        val migrated = migration.update(modernEntry)

        assertThat(migrated.data).isEqualTo(meta)
        // Not changed, for existing meta this filed should not be updated
        assertThat(migrated.errorMessage).isEqualTo(modernEntry.errorMessage)
    }

    @Test
    fun `updated - failed meta`() {
        val status = LoadTask.Status.Failed(nowMillis().minusSeconds(1), randomInt(), nowMillis(), randomString())
        val task = createTask(itemId, status)

        // Modern meta is outdated, should be migrated from legacy value
        val modernEntry = randomItemMetaDownloadEntry(
            status = DownloadStatus.FAILED,
            failedAt = status.failedAt.minusSeconds(1),
            data = null
        )

        val migration = FailedMetaMigration(task, modernEntry)
        val migrated = migration.update(modernEntry)

        assertThat(migrated.data).isNull()
        assertThat(migrated.errorMessage).isEqualTo(status.errorMessage)
    }

    @Test
    fun `migration not required - succeed but outdated`() = runBlocking<Unit> {
        val status = LoadTask.Status.Loaded(nowMillis().minusSeconds(1), randomInt(), nowMillis())
        val task = createTask(itemId, status)

        // Modern meta is outdated, nothing should happens
        val modernEntry = randomItemMetaDownloadEntry(
            status = DownloadStatus.SUCCESS,
            succeedAt = legacyEntry.cachedAt.plusSeconds(1)
        )
        val migration = DownloadedMetaMigration(task, modernEntry, legacyEntry)

        assertThat(migration.isMigrationRequired()).isEqualTo(false)
    }

    @Test
    fun `migration not required - failed to succeed`() = runBlocking<Unit> {
        val status = LoadTask.Status.Failed(nowMillis().minusSeconds(1), randomInt(), nowMillis(), "")
        val task = createTask(itemId, status)

        // Modern meta already has status SUCCEED, should be kept
        val modernEntry = randomItemMetaDownloadEntry(status = DownloadStatus.SUCCESS)
        val migration = DownloadedMetaMigration(task, modernEntry, legacyEntry)

        assertThat(migration.isMigrationRequired()).isEqualTo(false)
    }

    @Test
    fun `migration not required - failed, but outdated`() = runBlocking<Unit> {
        val status = LoadTask.Status.Failed(nowMillis().minusSeconds(1), randomInt(), nowMillis(), "")
        val task = createTask(itemId, status)

        val modernEntry = randomItemMetaDownloadEntry(
            status = DownloadStatus.FAILED,
            failedAt = status.failedAt.plusSeconds(1)
        )

        val migration = DownloadedMetaMigration(task, modernEntry, legacyEntry)

        assertThat(migration.isMigrationRequired()).isEqualTo(false)
    }

    private fun createTask(itemId: ItemIdDto, stats: LoadTask.Status): LoadTask {
        return LoadTask(LoadTaskId(), "", itemId.fullId(), stats, 1)
    }

}