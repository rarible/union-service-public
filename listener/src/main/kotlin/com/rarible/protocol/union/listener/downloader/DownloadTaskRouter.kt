package com.rarible.protocol.union.listener.downloader

import com.rarible.protocol.union.core.model.download.DownloadTask

/**
 * Router distributing tasks between several pipelines (from scheduler to executors)
 */
interface DownloadTaskRouter {

    suspend fun send(tasks: List<DownloadTask>, pipeline: String)

}