package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.protocol.union.core.model.ReconciliationMarkType
import com.rarible.protocol.union.enrichment.model.ReconciliationMark
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class ReconciliationMarkRepository(
    private val template: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val collection: String = template.getCollectionName(ReconciliationMark::class.java)

    suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun get(id: String): ReconciliationMark? {
        return template.findById<ReconciliationMark>(id).awaitFirstOrNull()
    }

    suspend fun save(mark: ReconciliationMark) {
        template.save(mark).awaitFirstOrNull()
    }

    suspend fun delete(mark: ReconciliationMark): DeleteResult? {
        return template.remove(mark).awaitFirstOrNull()
    }

    suspend fun findByType(type: ReconciliationMarkType, limit: Int): List<ReconciliationMark> {
        // Idea is to sort marks by retry counter in order to move failed records to the bottom of the list
        val query = Query(ReconciliationMark::type isEqualTo type)
            .limit(limit)
            .with(Sort.by(Sort.Direction.ASC, ReconciliationMark::retries.name))
        return template.find(query, ReconciliationMark::class.java).collectList().awaitFirst()
    }

    companion object {

        private val TYPE_RETRIES_DEFINITION = Index()
            .on(ReconciliationMark::type.name, Sort.Direction.ASC)
            .on(ReconciliationMark::retries.name, Sort.Direction.ASC)
            .background()

        private val ALL_INDEXES = listOf(
            TYPE_RETRIES_DEFINITION
        )
    }
}
