package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.configuration.EsOptimizationProperties
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.elastic.EsItemFilter
import com.rarible.protocol.union.core.model.elastic.EsItemGenericFilter
import com.rarible.protocol.union.core.model.elastic.EsItemLite
import com.rarible.protocol.union.core.model.elastic.EsItemSort
import com.rarible.protocol.union.core.model.elastic.EsItemSortType
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.internal.EsEntitySearchAfterCursorService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class EsItemOptimizedSearchService(
    private val esItemRepository: EsItemRepository,
    private val esEntitySearchAfterCursorService: EsEntitySearchAfterCursorService,
    properties: EsOptimizationProperties,
    clock: Clock,
    private val ff: FeatureFlagsProperties,
) : AbstractOptimizedSearchService<EsItemLite, EsItemGenericFilter, EsItemSort>(
    clock = clock,
    earliestDate = properties.earliestItemByLastUpdateAt,
    searchPeriod = properties.lastUpdatedSearchPeriod,
) {
    suspend fun search(filter: EsItemFilter, sort: EsItemSort, limit: Int?): Slice<EsItemLite> {
        if (ff.enableOptimizedSearchForItems.not()) {
            return esItemRepository.search(filter, sort, limit)
        }
        val max = PageSize.ITEM.limit(limit)
        return when (sort.type) {
            EsItemSortType.LATEST_FIRST,
            EsItemSortType.EARLIEST_FIRST -> {
                if (filter is EsItemGenericFilter) {
                    optimizeSearchIfSupported(filter, sort, max)
                } else {
                    esItemRepository.search(filter, sort, limit)
                }
            }
            EsItemSortType.RECENTLY_LISTED,
            EsItemSortType.RELEVANCE,
            EsItemSortType.TRAIT,
            EsItemSortType.HIGHEST_SELL_PRICE_FIRST,
            EsItemSortType.LOWEST_SELL_PRICE_FIRST,
            EsItemSortType.HIGHEST_BID_PRICE_FIRST,
            EsItemSortType.LOWEST_BID_PRICE_FIRST -> {
                esItemRepository.search(filter, sort, max)
            }
        }
    }

    override fun getCursorFromDate(filter: EsItemGenericFilter): Instant? {
        return try {
            val sortedFiled = esEntitySearchAfterCursorService.buildSearchAfterClause(filter.cursor, 2) ?: return null
            Instant.ofEpochMilli(sortedFiled.first().toString().toLong())
        } catch (ex: Throwable) {
            logger.warn("Can't parse lastUpdated cursor: ${filter.cursor}", ex)
            null
        }
    }

    override fun canBeOptimized(filter: EsItemGenericFilter, sort: EsItemSort): Boolean {
        return filter.itemIds.isNullOrEmpty()
    }

    override suspend fun esSearch(filter: EsItemGenericFilter, sort: EsItemSort, limit: Int): Slice<EsItemLite> {
        return esItemRepository.search(filter, sort, limit)
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(EsItemOptimizedSearchService::class.java)
    }
}
