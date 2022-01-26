package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.enrichment.model.ItemReconciliationMark
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.DB)
class ItemReconciliationMarkRepository(
    private val template: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun findAll(limit: Int): List<ItemReconciliationMark> {
        val query = Query().limit(limit)
        return template.find(query, ItemReconciliationMark::class.java).collectList().awaitFirst()
    }

    suspend fun save(mark: ItemReconciliationMark) {
        template.save(mark).awaitFirstOrNull()
    }

    suspend fun delete(mark: ItemReconciliationMark): DeleteResult? {
        return template.remove(mark).awaitFirstOrNull()
    }

}