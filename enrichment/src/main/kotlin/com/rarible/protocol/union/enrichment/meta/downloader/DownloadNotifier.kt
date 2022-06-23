package com.rarible.protocol.union.enrichment.meta.downloader

interface DownloadNotifier<T> {

    suspend fun notify(entry: DownloadEntry<T>)

}