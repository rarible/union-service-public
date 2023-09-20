package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.core.EsOptimizationProperties
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.elastic.EsOwnership
import com.rarible.protocol.union.core.model.elastic.EsOwnershipSort
import com.rarible.protocol.union.core.model.elastic.EsOwnershipsSearchFilter
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.enrichment.repository.search.internal.EsEntitySearchAfterCursorService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class EsOwnershipOptimizedSearchService(
    private val esEntitySearchAfterCursorService: EsEntitySearchAfterCursorService,
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val esOwnershipRepository: EsOwnershipRepository,
    properties: EsOptimizationProperties,
    clock: Clock,
) : AbstractOptimizedSearchService<EsOwnership, EsOwnershipsSearchFilter, EsOwnershipSort>(
    clock = clock,
    earliestDate = properties.earliestOwnershipDate,
    searchPeriod = properties.ownershipDateSearchPeriod,
) {

    suspend fun search(filter: EsOwnershipsSearchFilter, sort: EsOwnershipSort, limit: Int?): Slice<EsOwnership> {
        val max = PageSize.OWNERSHIP.limit(limit)
        return optimizeSearchIfSupported(filter, sort, max)
    }

    override fun getCursorFromDate(filter: EsOwnershipsSearchFilter): Instant? {
        return try {
            val sortedFiled = esEntitySearchAfterCursorService.buildSearchAfterClause(filter.cursor, 2) ?: return null
            Instant.ofEpochMilli(sortedFiled.first().toString().toLong())
        } catch (ex: Throwable) {
            logger.warn("Can't parse date cursor: ${filter.cursor}", ex)
            null
        }
    }

    override suspend fun esSearch(
        filter: EsOwnershipsSearchFilter,
        sort: EsOwnershipSort,
        limit: Int
    ): Slice<EsOwnership> {
        return esOwnershipRepository.search(filter, sort, limit)
    }

    override fun canBeOptimized(filter: EsOwnershipsSearchFilter, sort: EsOwnershipSort): Boolean {
        return featureFlagsProperties.enableOptimizedSearchForOwnerships &&
            sort == EsOwnershipSort.EARLIEST_FIRST &&
            filter.collections.isNullOrEmpty() &&
            filter.owners.isNullOrEmpty() &&
            filter.items.isNullOrEmpty() &&
            filter.auctions.isNullOrEmpty() &&
            filter.auctionOwners.isNullOrEmpty()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EsOwnershipOptimizedSearchService::class.java)
    }
}
