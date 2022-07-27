package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.enrichment.model.CollectionStatistics
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.DB)
class CollectionStatisticsRepository(
    private val template: ReactiveMongoTemplate
) {

    private val collection: String = template.getCollectionName(CollectionStatistics::class.java)

    suspend fun save(collectionStatistics: CollectionStatistics): CollectionStatistics {
        return template.save(collectionStatistics).awaitSingle()
    }

    suspend fun get(collectionId: ShortCollectionId): CollectionStatistics? {
        return template.findById<CollectionStatistics>(collectionId).awaitSingleOrNull()
    }

    suspend fun delete(collectionId: ShortCollectionId): DeleteResult? {
        val criteria = where(CollectionStatistics::id).isEqualTo(collectionId)
        return template.remove(Query(criteria), collection).awaitSingleOrNull()
    }
}
