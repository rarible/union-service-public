package com.rarible.protocol.union.enrichment.meta.downloader

import com.rarible.protocol.union.enrichment.download.DownloadEntry

interface DownloadEntryRepository<T> {

    /**
     *  Since download entry might be a part of parent entity, we should use
     *  injected operations to check is update required and update operation itself in order
     *  to handle optimistic locks. Implementation of this method MUST handle optimistic locking.
     */
    suspend fun update(
        entryId: String,
        isUpdateRequired: (current: DownloadEntry<T>?) -> Boolean,
        updateEntry: (current: DownloadEntry<T>?) -> DownloadEntry<T>
    ): DownloadEntry<T>?

    /**
     * Non-conditional update of entity which are parent of the download entry.
     */
    suspend fun update(
        entryId: String,
        updateEntry: (current: DownloadEntry<T>?) -> DownloadEntry<T>
    ): DownloadEntry<T>? = update(entryId, { true }, updateEntry)

    suspend fun get(id: String): DownloadEntry<T>?

    suspend fun getAll(ids: Collection<String>): List<DownloadEntry<T>>
}
