package com.rarible.protocol.union.enrichment.meta.embedded

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.DB)
class MongoEmbeddedContentStorage(
    private val template: ReactiveMongoTemplate
) : EmbeddedContentStorage {

    val collection: String = template.getCollectionName(UnionEmbeddedContent::class.java)

    override suspend fun get(id: String): UnionEmbeddedContent? {
        return template.findById<UnionEmbeddedContent>(id).awaitFirstOrNull()
    }

    override suspend fun save(content: UnionEmbeddedContent) {
        template.save(content).awaitFirst()
    }

    override suspend fun delete(id: String) {
        val criteria = Criteria("_id").isEqualTo(id)
        template.remove(Query(criteria), collection).awaitFirstOrNull()
    }

}