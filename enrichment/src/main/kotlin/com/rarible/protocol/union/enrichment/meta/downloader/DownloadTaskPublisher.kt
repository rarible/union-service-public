package com.rarible.protocol.union.enrichment.meta.downloader

import com.rarible.protocol.union.enrichment.meta.downloader.model.DownloadTask

/**
 * Publisher bringing tasks to the DownloadScheduler.
 */
interface DownloadTaskPublisher {

    suspend fun publish(tasks: List<DownloadTask>)

}