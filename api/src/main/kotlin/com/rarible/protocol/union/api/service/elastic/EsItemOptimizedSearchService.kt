package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.configuration.EsOptimizationProperties
import com.rarible.protocol.union.core.model.elastic.EsItemFilter
import com.rarible.protocol.union.core.model.elastic.EsItemGenericFilter
import com.rarible.protocol.union.core.model.elastic.EsItemLite
import com.rarible.protocol.union.core.model.elastic.EsItemSort
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.internal.EsEntitySearchAfterCursorService
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.search.sort.SortOrder.ASC
import org.elasticsearch.search.sort.SortOrder.DESC
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class EsItemOptimizedSearchService(
    private val esItemRepository: EsItemRepository,
    private val esEntitySearchAfterCursorService: EsEntitySearchAfterCursorService,
    private val properties: EsOptimizationProperties,
) {
    suspend fun search(filter: EsItemFilter, sort: EsItemSort, limit: Int?): Slice<EsItemLite> {
        val max = PageSize.ITEM.limit(limit)
        return when (sort) {
            EsItemSort.LATEST_FIRST,
            EsItemSort.EARLIEST_FIRST -> {
                optimizeSortByLastUpdated(filter, sort, max)
            }
            EsItemSort.HIGHEST_SELL_PRICE_FIRST,
            EsItemSort.LOWEST_SELL_PRICE_FIRST,
            EsItemSort.HIGHEST_BID_PRICE_FIRST,
            EsItemSort.LOWEST_BID_PRICE_FIRST -> {
                esItemRepository.search(filter, sort, max)
            }
        }
    }

    private suspend fun optimizeSortByLastUpdated(
        filter: EsItemFilter,
        sort: EsItemSort,
        limit: Int,
    ): Slice<EsItemLite> {
        return when (filter) {
            is EsItemGenericFilter -> optimizeSortByLastUpdatedGeneric(filter, sort, limit)
        }
    }

    private suspend fun optimizeSortByLastUpdatedGeneric(
        filter: EsItemGenericFilter,
        sort: EsItemSort,
        limit: Int,
    ): Slice<EsItemLite> {
        val lastUpdatedRange = getOptimizeFromAndTo(
            filterFrom = filter.updatedFrom,
            filterTo = filter.updatedTo,
            cursor = filter.cursor,
            sort = sort.sortOrder,
        )
        val optimizedFilter = filter.copy(
            updatedFrom = lastUpdatedRange.from,
            updatedTo = lastUpdatedRange.to,
        )
        val result = esItemRepository.search(optimizedFilter, sort, limit)

        if (result.entities.size >= limit &&
            result.continuation != null
        ) {
            return result
        }
        return esItemRepository.search(filter, sort, limit)
    }

    private fun getOptimizeFromAndTo(
        filterFrom: Instant?,
        filterTo: Instant?,
        cursor: String?,
        sort: SortOrder,
    ): LastUpdatedRange {
        val from = filterFrom ?: Instant.EPOCH
        val to = filterTo ?: Instant.now()
        return when (sort) {
            ASC -> {
                val currentFrom = getCursorFrom(cursor) ?: from
                val optimalTo = currentFrom.plus(properties.lastUpdatedSearchPeriod)
                LastUpdatedRange(from = currentFrom, to = minOf(to, optimalTo))
            }
            DESC -> {
                val currentTo = getCursorFrom(cursor) ?: to
                val optimalFrom = currentTo.minus(properties.lastUpdatedSearchPeriod)
                LastUpdatedRange(from = maxOf(from, optimalFrom), to = currentTo)
            }
        }
    }

    private fun getCursorFrom(cursor: String?): Instant? {
        return try {
            val sortedFiled = esEntitySearchAfterCursorService.buildSearchAfterClause(cursor, 2) ?: return null
            Instant.ofEpochMilli(sortedFiled.first().toString().toLong())
        } catch (ex: Throwable) {
            logger.warn("Can't parse lastUpdated cursor: $cursor", ex)
            null
        }
    }

    private data class LastUpdatedRange(
        val from: Instant,
        val to: Instant
    )

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(EsItemOptimizedSearchService::class.java)
    }
}
