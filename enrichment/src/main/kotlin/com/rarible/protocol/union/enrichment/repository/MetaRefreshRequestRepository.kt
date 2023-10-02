package com.rarible.protocol.union.enrichment.repository

import com.rarible.protocol.union.enrichment.model.MetaRefreshRequest
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
class MetaRefreshRequestRepository(
    private val template: ReactiveMongoTemplate
) {
    private val collection: String = template.getCollectionName(MetaRefreshRequest::class.java)

    suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun deleteCreatedBefore(date: Instant) {
        template.remove<MetaRefreshRequest>(
            Query(
                where(MetaRefreshRequest::createdAt).lt(date)
                    .and(MetaRefreshRequest::scheduled).isEqualTo(true)
            )
        )
            .awaitSingleOrNull()
    }

    suspend fun deleteAll() {
        template.remove<MetaRefreshRequest>(Query()).awaitSingleOrNull()
    }

    suspend fun countForCollectionId(collectionId: String): Long =
        template.count<MetaRefreshRequest>(
            Query(
                where(MetaRefreshRequest::collectionId).isEqualTo(collectionId)
            )
        ).awaitSingle()

    suspend fun countNotScheduledForCollectionId(collectionId: String): Long =
        template.count<MetaRefreshRequest>(
            Query(
                where(MetaRefreshRequest::collectionId).isEqualTo(collectionId)
                    .and(MetaRefreshRequest::scheduled).isEqualTo(false)
            )
        ).awaitSingle()

    suspend fun countNotScheduled(): Long =
        template.count<MetaRefreshRequest>(
            Query(
                where(MetaRefreshRequest::scheduled).isEqualTo(false)
            )
        ).awaitSingle()

    suspend fun save(request: MetaRefreshRequest) {
        template.save(request).awaitSingle()
    }

    suspend fun findToScheduleAndUpdate(size: Int): List<MetaRefreshRequest> {
        val requests = template.find(
            Query(
                where(MetaRefreshRequest::scheduled).isEqualTo(false)
                    .and(MetaRefreshRequest::scheduledAt).lt(Instant.now())
            ).with(
                Sort.by(MetaRefreshRequest::priority.name).descending()
                    .and(Sort.by(MetaRefreshRequest::createdAt.name).ascending())
            ).limit(size),
            MetaRefreshRequest::class.java
        ).asFlow().toList()
        template.updateMulti(
            Query(where(MetaRefreshRequest::id).inValues(requests.map { it.id })),
            Update().set(MetaRefreshRequest::scheduled.name, true),
            MetaRefreshRequest::class.java
        ).awaitSingle()
        return requests
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MetaRefreshRequestRepository::class.java)

        private val SCHEDULED_CREATED_AT_DEFINITION = Index()
            .on(MetaRefreshRequest::scheduled.name, Sort.Direction.ASC)
            .on(MetaRefreshRequest::createdAt.name, Sort.Direction.ASC)
            .background()

        private val COLLECTION_ID_DEFINITION = Index()
            .on(MetaRefreshRequest::collectionId.name, Sort.Direction.ASC)
            .background()

        private val ALL_INDEXES = listOf(
            SCHEDULED_CREATED_AT_DEFINITION,
            COLLECTION_ID_DEFINITION,
        )
    }
}
