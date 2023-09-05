package com.rarible.protocol.union.enrichment.meta.downloader

import com.rarible.protocol.union.enrichment.download.DownloadException

/**
 * Interface for downloaders, who perform log-running operations to fetch data
 */
interface Downloader<T> {

    /**
     * Return downloaded data OR throws DownloadException.
     * All other exceptions should be caught inside this method and replaced/wrapped by DownloadException
     */
    @Throws(DownloadException::class)
    suspend fun download(id: String): T

    val type: String
}
