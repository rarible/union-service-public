package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.enrichment.model.ItemReconciliationMark
import com.rarible.protocol.union.enrichment.model.ShortItemId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.DB)
class ItemReconciliationMarkRepository(
    private val template: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val collection: String = template.getCollectionName(ItemReconciliationMark::class.java)

    suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun get(id: ShortItemId): ItemReconciliationMark? {
        return template.findById<ItemReconciliationMark>(id).awaitFirstOrNull()
    }

    suspend fun save(mark: ItemReconciliationMark) {
        template.save(mark).awaitFirstOrNull()
    }

    suspend fun delete(mark: ItemReconciliationMark): DeleteResult? {
        return template.remove(mark).awaitFirstOrNull()
    }

    suspend fun findAll(limit: Int): List<ItemReconciliationMark> {
        // Idea is to sort marks by retry counter in order to move failed records to the bottom of the list
        val query = Query()
            .limit(limit)
            .with(Sort.by(Sort.Direction.ASC, ItemReconciliationMark::retries.name))
        return template.find(query, ItemReconciliationMark::class.java).collectList().awaitFirst()
    }

    companion object {

        private val RETRIES_DEFINITION = Index()
            .on(ItemReconciliationMark::retries.name, Sort.Direction.ASC)
            .background()

        private val ALL_INDEXES = listOf(
            RETRIES_DEFINITION
        )
    }
}