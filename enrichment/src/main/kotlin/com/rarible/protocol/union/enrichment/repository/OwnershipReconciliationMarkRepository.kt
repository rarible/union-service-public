package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.enrichment.model.OwnershipReconciliationMark
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.DB)
class OwnershipReconciliationMarkRepository(
    private val template: ReactiveMongoTemplate
) {

    suspend fun findAll(limit: Int): List<OwnershipReconciliationMark> {
        val query = Query().limit(limit)
        return template.find(query, OwnershipReconciliationMark::class.java).collectList().awaitFirst()
    }

    suspend fun delete(mark: OwnershipReconciliationMark): DeleteResult? {
        return template.remove(mark).awaitFirstOrNull()
    }

}