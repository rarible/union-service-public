package com.rarible.protocol.union.enrichment.meta.downloader

import com.rarible.protocol.union.core.model.download.DownloadEntry

interface DownloadEntryRepository<T> {

    suspend fun save(entry: DownloadEntry<T>): DownloadEntry<T>

    suspend fun get(id: String): DownloadEntry<T>?

    suspend fun getAll(ids: Collection<String>): List<DownloadEntry<T>>
}