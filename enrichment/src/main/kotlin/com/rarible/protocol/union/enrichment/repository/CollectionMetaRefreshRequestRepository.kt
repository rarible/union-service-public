package com.rarible.protocol.union.enrichment.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.enrichment.model.CollectionMetaRefreshRequest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@CaptureSpan(type = SpanType.DB)
class CollectionMetaRefreshRequestRepository(
    private val template: ReactiveMongoTemplate
) {
    private val collection: String = template.getCollectionName(CollectionMetaRefreshRequest::class.java)

    suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun deleteCreatedBefore(date: Instant) {
        template.remove<CollectionMetaRefreshRequest>(
            Query(
                where(CollectionMetaRefreshRequest::createdAt).lt(date)
                    .and(CollectionMetaRefreshRequest::scheduled).isEqualTo(true)
            )
        )
            .awaitSingleOrNull()
    }

    suspend fun deleteAll() {
        template.remove<CollectionMetaRefreshRequest>(Query()).awaitSingleOrNull()
    }

    suspend fun countForCollectionId(collectionId: String): Long =
        template.count<CollectionMetaRefreshRequest>(
            Query(
                where(CollectionMetaRefreshRequest::collectionId).isEqualTo(collectionId)
            )
        ).awaitSingle()

    suspend fun countNotScheduledForCollectionId(collectionId: String): Long =
        template.count<CollectionMetaRefreshRequest>(
            Query(
                where(CollectionMetaRefreshRequest::collectionId).isEqualTo(collectionId)
                    .and(CollectionMetaRefreshRequest::scheduled).isEqualTo(false)
            )
        ).awaitSingle()

    suspend fun countNotScheduled(): Long =
        template.count<CollectionMetaRefreshRequest>(
            Query(
                where(CollectionMetaRefreshRequest::scheduled).isEqualTo(false)
            )
        ).awaitSingle()

    suspend fun save(request: CollectionMetaRefreshRequest) {
        template.save(request).awaitSingle()
    }

    suspend fun findToScheduleAndUpdate(size: Int): List<CollectionMetaRefreshRequest> {
        val requests = template.find(
            Query(
                where(CollectionMetaRefreshRequest::scheduled).isEqualTo(false)
                    .and(CollectionMetaRefreshRequest::scheduledAt).lt(Instant.now())
            ).with(
                Sort.by(
                    CollectionMetaRefreshRequest::createdAt.name
                )
            ).limit(size),
            CollectionMetaRefreshRequest::class.java
        ).asFlow().toList()
        template.updateMulti(
            Query(where(CollectionMetaRefreshRequest::id).inValues(requests.map { it.id })),
            Update().set(CollectionMetaRefreshRequest::scheduled.name, true),
            CollectionMetaRefreshRequest::class.java
        ).awaitSingle()
        return requests
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CollectionMetaRefreshRequestRepository::class.java)

        private val SCHEDULED_CREATED_AT_DEFINITION = Index()
            .on(CollectionMetaRefreshRequest::scheduled.name, Sort.Direction.ASC)
            .on(CollectionMetaRefreshRequest::createdAt.name, Sort.Direction.ASC)
            .background()

        private val COLLECTION_ID_DEFINITION = Index()
            .on(CollectionMetaRefreshRequest::collectionId.name, Sort.Direction.ASC)
            .background()

        private val ALL_INDEXES = listOf(
            SCHEDULED_CREATED_AT_DEFINITION,
            COLLECTION_ID_DEFINITION,
        )
    }
}