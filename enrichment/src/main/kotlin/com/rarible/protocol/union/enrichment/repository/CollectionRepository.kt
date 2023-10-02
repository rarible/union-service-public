package com.rarible.protocol.union.enrichment.repository

import com.rarible.core.mongo.util.div
import com.rarible.protocol.union.enrichment.download.DownloadEntry
import com.rarible.protocol.union.enrichment.download.DownloadStatus
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
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
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import org.springframework.data.mongodb.core.query.lte
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class CollectionRepository(
    private val template: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(CollectionRepository::class.java)

    private val collection: String = template.getCollectionName(EnrichmentCollection::class.java)

    suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun save(collection: EnrichmentCollection): EnrichmentCollection {
        return template.save(collection).awaitFirst()
    }

    suspend fun get(collectionId: EnrichmentCollectionId): EnrichmentCollection? {
        return template.findById<EnrichmentCollection>(collectionId).awaitFirstOrNull()
    }

    suspend fun getAll(ids: List<EnrichmentCollectionId>): List<EnrichmentCollection> {
        val criteria = Criteria("_id").inValues(ids)
        return template.find<EnrichmentCollection>(Query(criteria)).collectList().awaitFirst()
    }

    fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<EnrichmentCollection> {
        val query = Query(
            Criteria().andOperator(
                EnrichmentCollection::multiCurrency isEqualTo true,
                EnrichmentCollection::lastUpdatedAt lte lastUpdateAt
            )
        ).withHint(MULTI_CURRENCY_DEFINITION.indexKeys)

        return template.find(query, EnrichmentCollection::class.java).asFlow()
    }

    suspend fun findIdsByLastUpdatedAt(
        lastUpdatedFrom: Instant,
        lastUpdatedTo: Instant,
        continuation: EnrichmentCollectionId?,
        size: Int = 20
    ): List<EnrichmentCollection> =
        template.find(
            Query(
                where(EnrichmentCollection::lastUpdatedAt).gt(lastUpdatedFrom).lte(lastUpdatedTo)
                    .apply {
                        if (continuation != null) {
                            and(EnrichmentCollection::id).gt(continuation)
                        }
                    }
            )
                .with(Sort.by(EnrichmentCollection::id.name))
                .limit(size),
            EnrichmentCollection::class.java
        ).collectList().awaitFirst()

    fun getCollectionsForMetaRetry(now: Instant, retryPeriod: Duration, attempt: Int): Flow<EnrichmentCollection> {
        val query = Query(
            Criteria().andOperator(
                EnrichmentCollection::metaEntry / DownloadEntry<*>::status isEqualTo DownloadStatus.RETRY,
                EnrichmentCollection::metaEntry / DownloadEntry<*>::retries isEqualTo attempt,
                EnrichmentCollection::metaEntry / DownloadEntry<*>::retriedAt lt now.minus(retryPeriod)
            )
        )

        return template.find(query, EnrichmentCollection::class.java).asFlow()
    }

    fun findAll(fromIdExcluded: EnrichmentCollectionId? = null): Flow<EnrichmentCollection> = template.find(
        Query(
            Criteria().apply {
                fromIdExcluded?.let { and(EnrichmentCollection::id).gt(fromIdExcluded) }
            }
        ).with(Sort.by(EnrichmentCollection::id.name)),
        EnrichmentCollection::class.java
    ).asFlow()

    companion object {

        private val STATUS_RETRIES_FAILED_AT_DEFINITION = Index()
            .partial(PartialIndexFilter.of(EnrichmentCollection::metaEntry / DownloadEntry<*>::status isEqualTo DownloadStatus.RETRY))
            .on("${EnrichmentCollection::metaEntry.name}.${DownloadEntry<*>::retries.name}", Sort.Direction.ASC)
            .on("${EnrichmentCollection::metaEntry.name}.${DownloadEntry<*>::retriedAt.name}", Sort.Direction.ASC)
            .background()

        private val BLOCKCHAIN_DEFINITION = Index()
            .on(EnrichmentCollection::blockchain.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        // TODO make it partial on multi-currency=true
        private val MULTI_CURRENCY_DEFINITION = Index()
            .on(EnrichmentCollection::multiCurrency.name, Sort.Direction.DESC)
            .on(EnrichmentCollection::lastUpdatedAt.name, Sort.Direction.DESC)
            .background()

        private val LAST_UPDATED_AT_ID: Index = Index()
            .on(EnrichmentCollection::lastUpdatedAt.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        private val ALL_INDEXES = listOf(
            BLOCKCHAIN_DEFINITION,
            MULTI_CURRENCY_DEFINITION,
            LAST_UPDATED_AT_ID,
            STATUS_RETRIES_FAILED_AT_DEFINITION
        )
    }
}
