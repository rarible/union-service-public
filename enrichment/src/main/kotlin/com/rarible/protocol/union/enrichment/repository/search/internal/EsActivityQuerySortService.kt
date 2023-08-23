package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivityCursor
import com.rarible.protocol.union.core.model.elastic.EsActivitySort
import com.rarible.protocol.union.core.EsOptimizationProperties
import com.rarible.protocol.union.core.FeatureFlagsProperties
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.DistanceFeatureQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Component
class EsActivityQuerySortService(
    private val esOptimizationProperties: EsOptimizationProperties,
    private val featureFlagsProperties: FeatureFlagsProperties,
) {
    var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneId.of("UTC"))

    fun applySort(
        query: BoolQueryBuilder,
        builder: NativeSearchQueryBuilder,
        sort: EsActivitySort,
        cursorAsString: String?,
        from: Instant?,
    ) {
        if (featureFlagsProperties.enableOptimizedSearchForActivities) {
            if (sort.latestFirst) {
                builder.sortByField(EsActivity::date, SortOrder.DESC)
                builder.sortByField(EsActivity::blockNumber, SortOrder.DESC)
                builder.sortByField(EsActivity::logIndex, SortOrder.DESC)
                builder.sortByField(EsActivity::salt, SortOrder.DESC)
            } else {
                val fromFilter = from ?: esOptimizationProperties.earliestActivityByDate
                val fromCursor = if (cursorAsString.isNullOrEmpty()) {
                    esOptimizationProperties.earliestActivityByDate
                } else {
                    EsActivityCursor.fromString(cursorAsString)?.date ?: esOptimizationProperties.earliestActivityByDate
                }
                val fromDate = Instant.ofEpochMilli(
                    max(
                        max(
                            fromFilter.toEpochMilli(),
                            fromCursor.toEpochMilli()
                        ),
                        esOptimizationProperties.earliestActivityByDate.toEpochMilli()
                    )
                )
                query.should(
                    QueryBuilders.distanceFeatureQuery(
                        "date",
                        DistanceFeatureQueryBuilder.Origin(formatter.format(fromDate)),
                        "1d"
                    )
                        .boost(1000.0f)
                )
                builder.sortByField("_score", SortOrder.DESC)
                builder.sortByField(EsActivity::date, SortOrder.ASC)
                builder.sortByField(EsActivity::blockNumber, SortOrder.ASC)
                builder.sortByField(EsActivity::logIndex, SortOrder.ASC)
                builder.sortByField(EsActivity::salt, SortOrder.ASC)
            }
        } else {
            val sortOrder = if (sort.latestFirst) SortOrder.DESC else SortOrder.ASC
            builder.sortByField(EsActivity::date, sortOrder)
            builder.sortByField(EsActivity::blockNumber, sortOrder)
            builder.sortByField(EsActivity::logIndex, sortOrder)
            builder.sortByField(EsActivity::salt, sortOrder)
        }
    }
}
