package com.rarible.protocol.union.enrichment.meta.downloader

import com.rarible.protocol.union.enrichment.download.DownloadEntry

/**
 * Notifier of successful downloads
 */
interface DownloadNotifier<T> {

    suspend fun notify(entry: DownloadEntry<T>)
}
