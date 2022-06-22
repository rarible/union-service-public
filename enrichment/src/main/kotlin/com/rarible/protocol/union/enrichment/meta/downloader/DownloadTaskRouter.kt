package com.rarible.protocol.union.enrichment.meta.downloader

import com.rarible.protocol.union.enrichment.meta.downloader.model.DownloadTask

/**
 * Router distributing tasks between several pipelines.
 */
interface DownloadTaskRouter {

    suspend fun send(tasks: List<DownloadTask>, pipeline: String)

}