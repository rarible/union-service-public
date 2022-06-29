package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
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
@CaptureSpan(type = SpanType.DB)
class CollectionRepositoryImpl(
    private val template: ReactiveMongoTemplate
) : CollectionRepository {

    private val logger = LoggerFactory.getLogger(ItemRepositoryImpl::class.java)

    private val collection: String = template.getCollectionName(ShortCollection::class.java)

    override suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    override suspend fun save(collection: ShortCollection): ShortCollection {
        return template.save(collection).awaitFirst()
    }

    override suspend fun get(collectionId: ShortCollectionId): ShortCollection? {
        return template.findById<ShortCollection>(collectionId).awaitFirstOrNull()
    }

    override suspend fun delete(collectionId: ShortCollectionId): DeleteResult? {
        val criteria = Criteria("_id").isEqualTo(collectionId)
        return template.remove(Query(criteria), collection).awaitFirstOrNull()
    }

    override suspend fun getAll(ids: List<ShortCollectionId>): List<ShortCollection> {
        val criteria = Criteria("_id").inValues(ids)
        return template.find<ShortCollection>(Query(criteria)).collectList().awaitFirst()
    }

    override fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<ShortCollection> {
        val query = Query(
            Criteria().andOperator(
                ShortCollection::multiCurrency isEqualTo true,
                ShortCollection::lastUpdatedAt lte lastUpdateAt
            )
        ).withHint(MULTI_CURRENCY_DEFINITION.indexKeys)

        return template.find(query, ShortCollection::class.java).asFlow()
    }

    companion object {

        private val BLOCKCHAIN_DEFINITION = Index()
            .on(ShortCollection::blockchain.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        private val MULTI_CURRENCY_DEFINITION = Index()
            .on(ShortCollection::multiCurrency.name, Sort.Direction.DESC)
            .on(ShortCollection::lastUpdatedAt.name, Sort.Direction.DESC)
            .background()

        private val ALL_INDEXES = listOf(
            BLOCKCHAIN_DEFINITION,
            MULTI_CURRENCY_DEFINITION
        )
    }

}
