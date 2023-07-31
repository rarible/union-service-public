package com.rarible.protocol.union.enrichment.meta.downloader

import com.rarible.protocol.union.core.model.download.DownloadEntry

/**
 * Notifier of successful downloads
 */
interface DownloadNotifier<T> {

    suspend fun notify(entry: DownloadEntry<T>)
}
