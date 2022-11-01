package com.rarible.protocol.union.core.model.download

import com.rarible.core.common.nowMillis
import java.time.Instant

data class DownloadEntry<T>(
    val id: String,
    val status: DownloadStatus,

    val data: T? = null,
    // Successful downloads
    val downloads: Int = 0,
    // Failed downloads
    val fails: Int = 0,
    // Current counter of retries
    val retries: Int = 0,
    // When task scheduled last time,
    // should not affect updatedAt
    val scheduledAt: Instant? = null,
    // When entry updated last time,
    // should be changed only on success/fail
    val updatedAt: Instant? = null,
    // When was the last successful load/refresh
    val succeedAt: Instant? = null,
    // When was the last fail
    val failedAt: Instant? = null,
    // Error message
    val errorMessage: String? = null
) {

    fun withFailInc(errorMessage: String?): DownloadEntry<T> {
        val now = nowMillis()
        return copy(
            failedAt = now,
            updatedAt = now,
            fails = fails.inc(),
            errorMessage = errorMessage
        )
    }

    fun withSuccessInc(data: T): DownloadEntry<T> {
        val now = nowMillis()
        return copy(
            succeedAt = now,
            updatedAt = now,
            data = data,
            downloads = downloads.inc(),
            status = DownloadStatus.SUCCESS,
            errorMessage = null
        )
    }

    fun isDownloaded(): Boolean {
        return status == DownloadStatus.SUCCESS && data != null
    }
}

enum class DownloadStatus {
    SCHEDULED,  // Never loaded, will be loaded soon
    SUCCESS,    // Successfully downloaded
    RETRY,      // Waits for retry
    FAILED      // Completely failed
}