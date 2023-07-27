package com.rarible.protocol.union.enrichment.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.enrichment.model.MetaAutoRefreshState
import com.rarible.protocol.union.enrichment.model.MetaAutoRefreshStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@CaptureSpan(type = SpanType.DB)
class MetaAutoRefreshStateRepository(
    private val template: ReactiveMongoTemplate
) {
    private val collection: String = template.getCollectionName(MetaAutoRefreshState::class.java)

    suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun save(metaAutoRefreshState: MetaAutoRefreshState) {
        template.save(metaAutoRefreshState).awaitSingle()
    }

    fun loadToCheckCreated(fromDate: Instant): Flow<MetaAutoRefreshState> =
        template.find(
            Query(
                where(MetaAutoRefreshState::status).isEqualTo(MetaAutoRefreshStatus.CREATED)
                    .and(MetaAutoRefreshState::createdAt).gt(fromDate)
            )
                .noCursorTimeout()
                .cursorBatchSize(1),
            MetaAutoRefreshState::class.java
        ).asFlow()

    fun loadToCheckRefreshed(createFromDate: Instant, refreshedFromDate: Instant): Flow<MetaAutoRefreshState> =
        template.find(
            Query(
                where(MetaAutoRefreshState::status).isEqualTo(MetaAutoRefreshStatus.REFRESHED)
                    .and(MetaAutoRefreshState::createdAt).gt(createFromDate)
                    .and(MetaAutoRefreshState::lastRefreshedAt).gt(refreshedFromDate)
            )
                .noCursorTimeout()
                .cursorBatchSize(1),
            MetaAutoRefreshState::class.java
        ).asFlow()

    suspend fun delete(id: String) {
        template.remove(Query(where(MetaAutoRefreshState::id).isEqualTo(id)), MetaAutoRefreshState::class.java)
            .awaitSingleOrNull()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MetaAutoRefreshStateRepository::class.java)

        private val STATUS_CREATED_AT_DEFINITION = Index()
            .on(MetaAutoRefreshState::status.name, Sort.Direction.ASC)
            .on(MetaAutoRefreshState::createdAt.name, Sort.Direction.ASC)
            .background()

        private val ALL_INDEXES = listOf(
            STATUS_CREATED_AT_DEFINITION,
        )
    }
}
