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
            failedAt = if (task.status.retryAttempts == 0) null else task.status.scheduledAt,
            retriedAt = if (task.status.retryAttempts == 0) null else task.status.scheduledAt.plusSeconds(15),
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

        // If modern record is not FAILED or RETRY - skip update (SCHEDULED or SUCCESS should not be updated in such case)
        if (modern.status != DownloadStatus.FAILED && modern.status != DownloadStatus.RETRY) {
            return false
        }

        // If fail date not specified or outdated - also should be updated
        if (modern.failedAt == null || modern.retriedAt == null || modern.failedAt!!.isBefore(status.failedAt)) {
            return true
        }
        return false
    }

    override fun update(modern: DownloadEntry<UnionMeta>?): DownloadEntry<UnionMeta> {
        val toUpdate = modern ?: createDefaultEntry()

        return toUpdate.copy(
            fails = status.retryAttempts,
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
            fails = status.retryAttempts,
            retries = status.retryAttempts,
            scheduledAt = status.scheduledAt,
            retriedAt = status.scheduledAt.plusSeconds(10),
            succeedAt = null,
            updatedAt = status.failedAt,
            errorMessage = trimToLength(status.errorMessage, MAX_ERROR_MESSAGE_LENGTH),
            failedAt = status.failedAt
        )
    }
}

@Deprecated("Should be removed after meta-pipeline migration")
class RetryMetaMigration(
    task: LoadTask,
    modernMeta: DownloadEntry<UnionMeta>?
) : ItemMetaMigration(
    task,
    modernMeta
) {

    private val status = task.status as LoadTask.Status.WaitsForRetry

    override fun isMigrationRequired(modern: DownloadEntry<UnionMeta>?): Boolean {
        // No record - let's create it
        if (modern == null) {
            return true
        }

        // We can update only RETRY entries here, no status transition allowed
        if (modern.data != null || modern.status != DownloadStatus.RETRY) {
            return false
        }

        return true
    }

    override fun update(modern: DownloadEntry<UnionMeta>?): DownloadEntry<UnionMeta> {
        val toUpdate = modern ?: createDefaultEntry()

        return toUpdate.copy(
            retries = status.retryAttempts,
            fails = status.retryAttempts,
            failedAt = status.failedAt,
            updatedAt = status.failedAt,
            errorMessage = trimToLength(status.errorMessage, MAX_ERROR_MESSAGE_LENGTH)
        )
    }

    private fun createDefaultEntry(): DownloadEntry<UnionMeta> {
        return DownloadEntry(
            id = task.key,
            status = DownloadStatus.RETRY,
            data = null,
            downloads = 0,
            fails = status.retryAttempts,
            retries = status.retryAttempts,
            scheduledAt = status.scheduledAt,
            retriedAt = status.scheduledAt.plusSeconds(10),
            succeedAt = null,
            updatedAt = status.failedAt,
            errorMessage = trimToLength(status.errorMessage, MAX_ERROR_MESSAGE_LENGTH),
            failedAt = status.failedAt
        )
    }

}