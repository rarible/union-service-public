package com.rarible.protocol.union.enrichment.service

import com.rarible.core.loader.internal.common.LoadTask
import com.rarible.core.loader.internal.common.LoadTaskUpdateListener
import com.rarible.loader.cache.internal.CacheRepository
import com.rarible.loader.cache.internal.MongoCacheEntry
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.util.optimisticLockWithInitial
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemMetaMigrator(
    private val legacyRepository: CacheRepository,
    private val modernRepository: ItemMetaRepository
) : LoadTaskUpdateListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onTaskSaved(task: LoadTask) {
        migrate(task)
    }

    /**
     * Single migration of the task, not applicable for batches. Should be used for realtime migrations
     */
    suspend fun migrate(task: LoadTask) {
        val migration = when (task.status) {
            is LoadTask.Status.Loaded -> {
                val legacy = legacyRepository.get<UnionMeta>(ItemMetaDownloader.TYPE, task.key)
                val modern = modernRepository.get(task.key)
                getDownloadedMigration(task, modern, legacy)
            }
            is LoadTask.Status.Failed -> {
                val modern = modernRepository.get(task.key)
                getFailedMigration(task, modern)
            }
            else -> null // Other statuses are not needed
        }
        migration?.migrate()
    }

    /**
     * Batch migration, should be used in background job
     */
    suspend fun migrate(tasks: List<LoadTask>, asyncTaskCount: Int) = coroutineScope {
        val filtered = tasks.filter { it.status is LoadTask.Status.Loaded || it.status is LoadTask.Status.Failed }
        val groupedByItem = filtered.groupBy { it.key }

        // We need legacy meta only for successful tasks
        val downloadedKeys = filtered.filter { it.status is LoadTask.Status.Loaded }.map { it.key }
        // But for modern meta we have to retrieve all entries in order to execute safe update
        val allKeys = groupedByItem.keys.toList()

        val modernMetaDeferred = async { modernRepository.getAll(allKeys).associateBy { it.id } }
        val legacyMeta = legacyRepository.getAll<UnionMeta>(ItemMetaDownloader.TYPE, downloadedKeys)
            .associateBy { it.key }
        val modernMeta = modernMetaDeferred.await()

        filtered.mapNotNull { task ->
            val modern = modernMeta[task.key]
            val legacy = legacyMeta[task.key]
            when (task.status) {
                is LoadTask.Status.Loaded -> getDownloadedMigration(task, modern, legacy)
                is LoadTask.Status.Failed -> getFailedMigration(task, modern)
                else -> null
            }
        }.filter { it.isMigrationRequired() }
            .chunked(asyncTaskCount)
            .map { chunk -> chunk.map { task -> async { task.migrate() } }.awaitAll() }
    }

    private fun getDownloadedMigration(
        task: LoadTask,
        modern: DownloadEntry<UnionMeta>?,
        legacy: MongoCacheEntry<UnionMeta>?
    ): DownloadedMetaMigration? {
        if (legacy == null) {
            logger.info("Meta with key [{}] not found not found for Loaded task [{}]", task.key, task.id)
            return null
        }
        return DownloadedMetaMigration(modernRepository, task, modern, legacy)
    }

    private fun getFailedMigration(
        task: LoadTask,
        modern: DownloadEntry<UnionMeta>?
    ): FailedMetaMigration {
        return FailedMetaMigration(modernRepository, task, modern)
    }

    private abstract class MetaMigration(
        protected val itemMetaRepository: ItemMetaRepository,
        protected val task: LoadTask,
        protected val modernMeta: DownloadEntry<UnionMeta>?
    ) {

        fun isMigrationRequired(): Boolean = isMigrationRequired(modernMeta)
        abstract fun isMigrationRequired(modern: DownloadEntry<UnionMeta>?): Boolean
        abstract suspend fun update(modern: DownloadEntry<UnionMeta>?): DownloadEntry<UnionMeta>

        suspend fun migrate() {
            optimisticLockWithInitial(modernMeta) { initial ->
                val itemId = task.key
                val modernMetaActual = initial ?: itemMetaRepository.get(itemId)
                if (!isMigrationRequired(modernMetaActual)) return@optimisticLockWithInitial

                val updated = update(modernMetaActual)
                itemMetaRepository.save(updated)
            }
        }
    }

    private class DownloadedMetaMigration(
        itemMetaRepository: ItemMetaRepository,
        task: LoadTask,
        modernMeta: DownloadEntry<UnionMeta>?,
        private val legacyMeta: MongoCacheEntry<UnionMeta>
    ) : MetaMigration(
        itemMetaRepository, task, modernMeta
    ) {

        override fun isMigrationRequired(modern: DownloadEntry<UnionMeta>?): Boolean {
            // There is no modern meta or it has no data (means status != SUCCEED)
            if (modern?.data == null) {
                return true
            }

            // Legacy meta has older date - migration required
            if (modern.succeedAt == null || legacyMeta.cachedAt.isAfter(modern.succeedAt)) {
                return true
            }
            // Already actual
            return false
        }

        override suspend fun update(modern: DownloadEntry<UnionMeta>?): DownloadEntry<UnionMeta> {
            val toUpdate = modernMeta ?: createDefaultEntry()

            return toUpdate.copy(
                status = DownloadStatus.SUCCESS,
                downloads = 1,
                succeedAt = legacyMeta.cachedAt,
                updatedAt = legacyMeta.cachedAt,
                data = legacyMeta.data
            )
        }

        private fun createDefaultEntry(): DownloadEntry<UnionMeta> {
            return DownloadEntry(
                id = task.key,
                status = DownloadStatus.SUCCESS,
                data = legacyMeta.data,
                downloads = 1,
                fails = task.status.retryAttempts,
                retries = task.status.retryAttempts,
                scheduledAt = task.status.scheduledAt,
                succeedAt = legacyMeta.cachedAt,
                updatedAt = legacyMeta.cachedAt,
                // Not a real date, but let it be consistent
                failedAt = if (task.status.retryAttempts == 0) null else task.status.scheduledAt
            )
        }
    }

    private class FailedMetaMigration(
        itemMetaRepository: ItemMetaRepository,
        task: LoadTask,
        modernMeta: DownloadEntry<UnionMeta>?
    ) : MetaMigration(
        itemMetaRepository,
        task,
        modernMeta
    ) {

        private val status = task.status as LoadTask.Status.Failed

        override fun isMigrationRequired(modern: DownloadEntry<UnionMeta>?): Boolean {
            // No record - let's create it
            if (modern == null) {
                return true
            }

            // If modern record is not FAILED for some reason - skip update
            if (modern.status != DownloadStatus.FAILED) {
                return false
            }

            // If fail date not specified or outdated - also should be updated
            val failedStatus = task.status as LoadTask.Status.Failed
            if (modern.failedAt == null || modern.failedAt!!.isBefore(failedStatus.failedAt)) {
                return true
            }
            return false
        }

        override suspend fun update(modern: DownloadEntry<UnionMeta>?): DownloadEntry<UnionMeta> {
            val toUpdate = modernMeta ?: createDefaultEntry()

            return toUpdate.copy(
                failedAt = status.failedAt,
                updatedAt = status.failedAt
            )
        }

        private fun createDefaultEntry(): DownloadEntry<UnionMeta> {
            return DownloadEntry(
                id = task.key,
                status = DownloadStatus.FAILED,
                data = null,
                downloads = 0,
                fails = task.status.retryAttempts,
                retries = task.status.retryAttempts,
                scheduledAt = task.status.scheduledAt,
                succeedAt = null,
                updatedAt = status.failedAt,
                errorMessage = status.errorMessage,
                failedAt = status.failedAt
            )
        }
    }

}

