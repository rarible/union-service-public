package com.rarible.protocol.union.listener.downloader

import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent

/**
 * Router distributing tasks between several pipelines (from scheduler to executors)
 */
interface DownloadTaskRouter {

    suspend fun send(tasks: List<DownloadTaskEvent>, pipeline: String)
}
