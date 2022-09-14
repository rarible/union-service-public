package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.EsOwnershipSort
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipSearchSortDto
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder
import org.springframework.stereotype.Service

@Service
class EsOwnershipQueryScoreService(
    private val esEntityQueryScoreService: EsEntityQueryScoreService,
) {

    suspend fun buildQuery(
        boolQuery: BoolQueryBuilder,
        sort: EsOwnershipSort,
        blockchains: Set<BlockchainDto>
    ): FunctionScoreQueryBuilder {
        return when (sort) {
            EsOwnershipSort.LATEST_FIRST,
            EsOwnershipSort.EARLIEST_FIRST -> throw UnsupportedOperationException("ScoreService can't be used with $sort")
            EsOwnershipSort.HIGHEST_SELL_PRICE_FIRST,
            EsOwnershipSort.LOWEST_SELL_PRICE_FIRST -> esEntityQueryScoreService.buildQuery(
                boolQuery,
                sort == EsOwnershipSort.HIGHEST_SELL_PRICE_FIRST,
                EsOwnership::bestSellCurrency.name,
                EsOwnership::bestSellAmount.name,
                blockchains
            )
        }
    }
}