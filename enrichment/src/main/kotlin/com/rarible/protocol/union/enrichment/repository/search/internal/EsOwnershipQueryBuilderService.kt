package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.EsOwnershipByItemFilter
import com.rarible.protocol.union.core.model.EsOwnershipByOwnerFilter
import com.rarible.protocol.union.core.model.EsOwnershipFilter
import com.rarible.protocol.union.core.model.EsOwnershipSort
import com.rarible.protocol.union.core.model.EsOwnershipsSearchFilter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UnionBlockchainId
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsOwnershipQueryBuilderService(
    private val cursorService: EsEntitySearchAfterCursorService,
    private val sortService: EsOwnershipQuerySortService,
    private val scoreService: EsOwnershipQueryScoreService,
    private val priceFilterService: EsOwnershipQueryPriceFilterService,
) {
    companion object {
        private val SCORE_SORT_TYPES: Set<EsOwnershipSort> = setOf(
            EsOwnershipSort.LOWEST_SELL_PRICE_FIRST,
            EsOwnershipSort.HIGHEST_SELL_PRICE_FIRST,
        )
    }

    suspend fun build(filter: EsOwnershipFilter, sort: EsOwnershipSort): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val isScoreSort = SCORE_SORT_TYPES.contains(sort)

        val blockchains = if (filter.blockchains.isNullOrEmpty()) {
            BlockchainDto.values().toSet()
        } else {
            filter.blockchains!!
        }

        var query: QueryBuilder = BoolQueryBuilder()
        query as BoolQueryBuilder

        when (filter) {
            is EsOwnershipByItemFilter -> query.applyByItemFilter(filter)
            is EsOwnershipByOwnerFilter -> query.applyByOwnerFilter(filter)
            is EsOwnershipsSearchFilter -> {
                query.applySearchFilter(filter)
                priceFilterService.applyPriceFilter(query, filter, blockchains)
            }
        }

        if (isScoreSort) {
            query = scoreService.buildQuery(query, sort, blockchains)
        }

        sortService.applySort(builder, sort)

        builder.withQuery(query)
        val resultQuery = builder.build()
        resultQuery.searchAfter = cursorService.buildSearchAfterClause(filter.cursor)
        return resultQuery
    }

    private fun BoolQueryBuilder.applyByItemFilter(filter: EsOwnershipByItemFilter) {
        mustMatchTerm(filter.itemId.fullId(), EsOwnership::itemId.name)
    }

    private fun BoolQueryBuilder.applyByOwnerFilter(filter: EsOwnershipByOwnerFilter) {
        mustMatchTerm(filter.owner.fullId(), EsOwnership::owner.name)
        mustMatchTerms(filter.blockchains?.toSet().orEmpty(), EsOwnership::blockchain.name)
    }

    private fun BoolQueryBuilder.applySearchFilter(filter: EsOwnershipsSearchFilter) {

        fun matchIdsList(idsList: List<UnionBlockchainId>?, fieldName: String) {
            idsList?.let { ids ->
                mustMatchTerms(ids.map { it.fullId() }.toSet(), fieldName)
            }
        }

        fun matchAddressesList(aList: List<UnionAddress>?, fieldName: String) {
            aList?.let { l ->
                mustMatchTerms(l.map { it.fullId() }.toSet(), fieldName)
            }
        }

        filter.blockchains?.let {
            mustMatchTerms(it, EsOwnership::blockchain.name)
        }

        matchAddressesList(filter.owners, EsOwnership::owner.name)
        matchIdsList(filter.items, EsOwnership::itemId.name)
        matchIdsList(filter.collections, EsOwnership::collection.name)
        matchIdsList(filter.auctions, EsOwnership::auctionId.name)
        matchAddressesList(filter.auctionOwners, EsOwnership::auctionOwnershipId.name)

        when {
            filter.beforeDate != null && filter.afterDate != null -> must(
                RangeQueryBuilder(EsOwnership::date.name).lt(
                    filter.beforeDate
                ).gt(filter.afterDate)
            )
            filter.beforeDate != null -> must(RangeQueryBuilder(EsOwnership::date.name).lt(filter.beforeDate))
            filter.afterDate != null -> must(RangeQueryBuilder(EsOwnership::date.name).gt(filter.afterDate))
        }
    }
}
