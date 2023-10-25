package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.configuration.EsOptimizationProperties
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.elastic.ElasticActivityFilter
import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivityCursor
import com.rarible.protocol.union.core.model.elastic.EsActivitySort
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class EsActivityOptimizedSearchService(
    private val esActivityRepository: EsActivityRepository,
    private val featureFlagsProperties: FeatureFlagsProperties,
    esOptimizationProperties: EsOptimizationProperties,
    clock: Clock,
) : AbstractOptimizedSearchService<EsActivity, ElasticActivityFilter, EsActivitySort>(
    clock = clock,
    earliestDate = esOptimizationProperties.earliestActivityByDate,
    searchPeriod = esOptimizationProperties.activityDateSearchPeriod,
) {

    suspend fun search(filter: ElasticActivityFilter, sort: EsActivitySort, limit: Int?): Slice<EsActivity> {
        val max = PageSize.ACTIVITY.limit(limit)
        return optimizeSearchIfSupported(filter, sort, max)
    }

    override fun getCursorFromDate(filter: ElasticActivityFilter): Instant? =
        filter.cursor?.let { EsActivityCursor.fromString(it)?.date }

    override suspend fun esSearch(filter: ElasticActivityFilter, sort: EsActivitySort, limit: Int): Slice<EsActivity> {
        val result = esActivityRepository.search(filter, sort, limit)
        return Slice(continuation = result.cursor, entities = result.activities)
    }

    override fun canBeOptimized(filter: ElasticActivityFilter, sort: EsActivitySort): Boolean =
        featureFlagsProperties.enableOptimizedSearchForActivities &&
            !sort.latestFirst &&
            filter.bidCurrencies.isEmpty() &&
            filter.anyUsers.isEmpty() &&
            filter.usersFrom.isEmpty() &&
            filter.usersTo.isEmpty() &&
            filter.collections.isEmpty() &&
            filter.items.isEmpty()
}
