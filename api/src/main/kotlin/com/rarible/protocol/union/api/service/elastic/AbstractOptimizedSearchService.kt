package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.core.model.elastic.DateRange
import com.rarible.protocol.union.core.model.elastic.DateRangeFilter
import com.rarible.protocol.union.core.model.elastic.OrderedSort
import com.rarible.protocol.union.dto.continuation.page.Slice
import org.elasticsearch.search.sort.SortOrder
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant

abstract class AbstractOptimizedSearchService<T, F : DateRangeFilter<F>, S : OrderedSort>(
    private val earliestDate: Instant,
    private val searchPeriod: Duration,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    protected suspend fun optimizeSearchIfSupported(
        filter: F,
        sort: S,
        limit: Int,
    ): Slice<T> {
        return if (canBeOptimized(filter, sort)) {
            optimizedSearch(filter, sort, limit)
        } else {
            esSearch(filter, sort, limit)
        }
    }

    private suspend fun optimizedSearch(
        filter: F,
        sort: S,
        limit: Int,
    ): Slice<T> {
        val dateRange = getOptimizeFromAndTo(
            filterFrom = filter.from,
            filterTo = filter.to,
            cursorFrom = getCursorFromDate(filter),
            sort = sort.sortOrder,
        )
        val optimizedFilter = filter.applyDateRange(
            range = dateRange
        )
        val result = esSearch(optimizedFilter, sort, limit)

        if (result.entities.size >= limit &&
            result.continuation != null ||
            optimizedFilter == filter
        ) {
            logger.info("Optimized search success: optimizedFilter: $optimizedFilter, filter: $filter")
            return result
        }
        logger.info("Optimized search failed: optimizedFilter: $optimizedFilter, filter: $filter")
        return esSearch(filter, sort, limit)
    }

    protected abstract fun getCursorFromDate(filter: F): Instant?
    protected abstract suspend fun esSearch(filter: F, sort: S, limit: Int): Slice<T>
    protected abstract fun canBeOptimized(filter: F, sort: S): Boolean

    private fun getOptimizeFromAndTo(
        filterFrom: Instant?,
        filterTo: Instant?,
        cursorFrom: Instant?,
        sort: SortOrder,
    ): DateRange {
        val from = filterFrom ?: earliestDate
        val to = filterTo ?: clock.instant()
        return when (sort) {
            SortOrder.ASC -> {
                val currentFrom = cursorFrom ?: from
                val optimalTo = currentFrom.plus(searchPeriod)
                DateRange(from = currentFrom, to = minOf(to, optimalTo))
            }
            SortOrder.DESC -> {
                val currentTo = cursorFrom ?: to
                val optimalFrom = currentTo.minus(searchPeriod)
                DateRange(from = maxOf(from, optimalFrom), to = currentTo)
            }
        }
    }
}
