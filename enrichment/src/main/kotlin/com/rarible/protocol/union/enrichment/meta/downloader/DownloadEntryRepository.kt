package com.rarible.protocol.union.enrichment.meta.downloader

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo

abstract class DownloadEntryRepository<T>(
    protected val template: ReactiveMongoTemplate
) {

    abstract val collection: String

    suspend fun save(entry: DownloadEntry<T>): DownloadEntry<T> {
        return template.save(entry).awaitFirst()
    }

    suspend fun get(id: String): DownloadEntry<T>? {
        return template.findById<DownloadEntry<T>>(id, collection).awaitFirstOrNull()
    }

    suspend fun getAll(ids: Collection<String>): List<DownloadEntry<T>> {
        val criteria = Criteria("_id").inValues(ids)
        return template.find<DownloadEntry<T>>(Query(criteria)).collectList().awaitFirst()
    }

    suspend fun delete(id: String): Boolean {
        val criteria = Criteria("_id").isEqualTo(id)
        val result = template.remove(criteria, collection).awaitSingle()
        return result.deletedCount > 0
    }

}