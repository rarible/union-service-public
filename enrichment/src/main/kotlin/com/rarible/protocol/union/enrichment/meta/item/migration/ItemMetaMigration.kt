package com.rarible.protocol.union.enrichment.meta.item.migration

import com.rarible.core.loader.internal.common.LoadTask
import com.rarible.loader.cache.internal.MongoCacheEntry
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadEntry.Companion.MAX_ERROR_MESSAGE_LENGTH
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.core.util.trimToLength

@Deprecated("Should be removed after meta-pipeline migration")
sealed class ItemMetaMigration(
    val task: LoadTask,
    val modernMeta: DownloadEntry<UnionMeta>?
) {

    // To check batch data we want to migrate
    fun isMigrationRequired(): Boolean = isMigrationRequired(modernMeta)

    // To check actual data right before update to exclude any concurrency cases
    abstract fun isMigrationRequired(modern: DownloadEntry<UnionMeta>?): Boolean

    // Conversion operation
    abstract fun update(modern: DownloadEntry<UnionMeta>?): DownloadEntry<UnionMeta>

}

@Deprecated("Should be removed after meta-pipeline migration")
class DownloadedMetaMigration(
    task: LoadTask,
    modernMeta: DownloadEntry<UnionMeta>?,
    private val legacyMeta: MongoCacheEntry<UnionMeta>
) : ItemMetaMigration(
    task, modernMeta
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

    override fun update(modern: DownloadEntry<UnionMeta>?): DownloadEntry<UnionMeta> {
        val toUpdate = modern ?: createDefaultEntry()

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

@Deprecated("Should be removed after meta-pipeline migration")
class FailedMetaMigration(
    task: LoadTask,
    modernMeta: DownloadEntry<UnionMeta>?
) : ItemMetaMigration(
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

    override fun update(modern: DownloadEntry<UnionMeta>?): DownloadEntry<UnionMeta> {
        val toUpdate = modern ?: createDefaultEntry()

        return toUpdate.copy(
            failedAt = status.failedAt,
            updatedAt = status.failedAt,
            errorMessage = trimToLength(status.errorMessage, MAX_ERROR_MESSAGE_LENGTH)
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
            errorMessage = trimToLength(status.errorMessage, MAX_ERROR_MESSAGE_LENGTH),
            failedAt = status.failedAt
        )
    }
}