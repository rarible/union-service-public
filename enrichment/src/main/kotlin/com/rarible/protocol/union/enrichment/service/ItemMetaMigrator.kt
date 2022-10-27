package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.optimisticLock
import com.rarible.core.loader.internal.common.LoadTask
import com.rarible.core.loader.internal.common.LoadTaskUpdateListener
import com.rarible.loader.cache.internal.CacheRepository
import com.rarible.loader.cache.internal.MongoCacheEntry
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import org.springframework.stereotype.Component

@Component
class ItemMetaMigrator(
    private val cacheRepository: CacheRepository,
    private val itemMetaRepository: ItemMetaRepository
) : LoadTaskUpdateListener {

    override suspend fun onTaskSaved(task: LoadTask) {
        migrate(task)
    }

    /**
     * Single migration of the task, not applicable for batches
     */
    suspend fun migrate(task: LoadTask) {
        when (task.status) {
            is LoadTask.Status.Loaded -> migrateDownloadedMeta(task)
            is LoadTask.Status.Failed -> migrateFailedMeta(task)
            else -> {} // Other statuses are not needed
        }
    }

    suspend fun migrate(tasks: List<LoadTask>) {
        val filtered = tasks.filter {
            it.status is LoadTask.Status.Loaded
                || it.status is LoadTask.Status.Failed
        }
        val groupedByItem = filtered.groupBy { it.key }

        val modernMeta = itemMetaRepository.getAll(groupedByItem.keys).associateBy { it.id }

    }

    private suspend fun migrateDownloadedMeta(task: LoadTask) {
        val itemId = task.key

        // Originally, if task state is Loaded, meta MUST be not-null
        val legacyMeta = cacheRepository.get<UnionMeta>(ItemMetaDownloader.TYPE, itemId) ?: return

        optimisticLock {
            val modernMeta = itemMetaRepository.get(itemId)
            if (!isDownloadedMetaMigrationRequired(legacyMeta, modernMeta)) {
                return@optimisticLock
            }

            val toUpdate = modernMeta ?: createDefaultSuccessEntry(task, legacyMeta)

            val updated = toUpdate.copy(
                status = DownloadStatus.SUCCESS,
                downloads = 1,
                succeedAt = legacyMeta.cachedAt,
                updatedAt = legacyMeta.cachedAt,
                data = legacyMeta.data
            )

            itemMetaRepository.save(updated)
        }
    }

    private fun isDownloadedMetaMigrationRequired(
        legacy: MongoCacheEntry<UnionMeta>,
        modern: DownloadEntry<UnionMeta>?
    ): Boolean {
        // There is no modern meta or it has no data
        if (modern?.data == null) {
            return true
        }
        // Legacy meta has older date - migration required
        if (modern.succeedAt == null || legacy.cachedAt.isAfter(modern.succeedAt)) {
            return true
        }
        // Already actual
        return false
    }

    private suspend fun migrateFailedMeta(task: LoadTask) {
        val itemId = task.key
        val failedStatus = task.status as LoadTask.Status.Failed
        optimisticLock {
            val modernMeta = itemMetaRepository.get(itemId)
            if (!isFailedMetaMigrationRequired(task, modernMeta)) {
                return@optimisticLock
            }

            val toUpdate = modernMeta ?: createDefaultFailedEntry(task)

            val updated = toUpdate.copy(
                failedAt = failedStatus.failedAt,
                updatedAt = failedStatus.failedAt
            )

            itemMetaRepository.save(updated)
        }
    }

    private fun isFailedMetaMigrationRequired(
        task: LoadTask,
        modern: DownloadEntry<UnionMeta>?
    ): Boolean {
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

    private fun createDefaultSuccessEntry(
        task: LoadTask,
        legacyMeta: MongoCacheEntry<UnionMeta>
    ): DownloadEntry<UnionMeta> {
        val itemId = task.key
        return DownloadEntry(
            id = itemId,
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

    private fun createDefaultFailedEntry(
        task: LoadTask
    ): DownloadEntry<UnionMeta> {
        val status = task.status as LoadTask.Status.Failed
        val itemId = task.key
        return DownloadEntry(
            id = itemId,
            status = DownloadStatus.FAILED,
            data = null,
            downloads = 0,
            fails = task.status.retryAttempts,
            retries = task.status.retryAttempts,
            scheduledAt = task.status.scheduledAt,
            succeedAt = null,
            updatedAt = status.failedAt,
            errorMessage = status.errorMessage,
            // Not a real date, but let it be consistent
            failedAt = status.failedAt
        )
    }
}