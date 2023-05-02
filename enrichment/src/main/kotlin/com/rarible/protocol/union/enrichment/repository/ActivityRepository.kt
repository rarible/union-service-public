package com.rarible.protocol.union.enrichment.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.model.EnrichmentActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentActivityId
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import kotlinx.coroutines.flow.toList
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
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.DB)
class ActivityRepository(
    private val template: ReactiveMongoTemplate
) {
    private val collection: String = template.getCollectionName(EnrichmentCollection::class.java)

    suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun <T : EnrichmentActivity> save(activity: T): T {
        return template.save(activity).awaitFirst()
    }

    suspend fun get(activityId: EnrichmentActivityId): EnrichmentActivity? {
        return template.findById<EnrichmentActivity>(activityId).awaitFirstOrNull()
    }

    suspend fun getAll(activityIds: List<EnrichmentActivityId>): List<EnrichmentActivity> {
        val activitiesByIds =
            template.find<EnrichmentActivity>(Query(Criteria("_id").inValues(activityIds))).asFlow().toList()
                .associateBy { it.id }
        return activityIds.mapNotNull { activitiesByIds[it] }
    }

    suspend fun findLastSale(itemId: ItemIdDto): EnrichmentActivity? =
        template.find<EnrichmentActivity>(
            Query(
                where(EnrichmentActivity::itemId).isEqualTo(itemId)
                    .and(EnrichmentActivity::activityType).isEqualTo(ActivityTypeDto.SELL)
            ).with(
                Sort.by(
                    Sort.Order.desc(EnrichmentActivity::date.name),
                    Sort.Order.desc("_id"),
                )
            )
        ).awaitFirstOrNull()

    suspend fun delete(activityId: EnrichmentActivityId) {
        template.remove(Query(Criteria("_id").isEqualTo(activityId)), EnrichmentActivity::class.java)
            .awaitFirstOrNull()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ActivityRepository::class.java)

        private val LAST_SALE_DEFINITION = Index()
            .partial(PartialIndexFilter.of(EnrichmentActivity::activityType isEqualTo ActivityTypeDto.SELL))
            .on(EnrichmentActivity::itemId.name, Sort.Direction.ASC)
            .on(EnrichmentActivity::date.name, Sort.Direction.DESC)
            .on("_id", Sort.Direction.DESC)
            .background()

        private val ALL_INDEXES = listOf(
            LAST_SALE_DEFINITION
        )
    }
}