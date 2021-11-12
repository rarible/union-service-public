package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lte
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@CaptureSpan(type = "db")
class ItemRepository(
    private val template: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(ItemRepository::class.java)

    val collection: String = template.getCollectionName(ShortItem::class.java)

    suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun save(item: ShortItem): ShortItem {
        return template.save(item).awaitFirst()
    }

    suspend fun get(id: ShortItemId): ShortItem? {
        return template.findById<ShortItem>(id).awaitFirstOrNull()
    }

    suspend fun findAll(ids: List<ShortItemId>): List<ShortItem> {
        val criteria = Criteria("_id").inValues(ids)
        return template.find<ShortItem>(Query(criteria)).collectList().awaitFirst()
    }

    fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<ShortItem> {
        val query = Query(
            Criteria().andOperator(
                ShortItem::multiCurrency isEqualTo true,
                ShortItem::lastUpdatedAt lte lastUpdateAt
            )
        ).withHint(MULTI_CURRENCY_DEFINITION.indexKeys)

        return template.find(query, ShortItem::class.java).asFlow()
    }

    suspend fun delete(itemId: ShortItemId): DeleteResult? {
        val criteria = Criteria("_id").isEqualTo(itemId)
        return template.remove(Query(criteria), collection).awaitFirstOrNull()
    }

    companion object {
        private val MULTI_CURRENCY_DEFINITION = Index()
            .on(ShortItem::multiCurrency.name, Sort.Direction.DESC)
            .on(ShortItem::lastUpdatedAt.name, Sort.Direction.DESC)
            .background()

        private val ALL_INDEXES = listOf(
            MULTI_CURRENCY_DEFINITION
        )
    }
}
