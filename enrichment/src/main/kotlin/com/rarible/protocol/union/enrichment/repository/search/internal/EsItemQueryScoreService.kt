package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsItemSort
import com.rarible.protocol.union.dto.BlockchainDto
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder
import org.springframework.stereotype.Service

@Service
class EsItemQueryScoreService(
    private val esEntityQueryScoreService: EsEntityQueryScoreService,
) {

    suspend fun buildQuery(
        boolQuery: BoolQueryBuilder,
        sort: EsItemSort,
        blockchains: Set<BlockchainDto>
    ): FunctionScoreQueryBuilder {
        return when (sort) {
            EsItemSort.LATEST_FIRST,
            EsItemSort.EARLIEST_FIRST -> throw UnsupportedOperationException("ScoreService can't be used with $sort")
            EsItemSort.HIGHEST_SELL_PRICE_FIRST,
            EsItemSort.LOWEST_SELL_PRICE_FIRST -> esEntityQueryScoreService.buildQuery(
                boolQuery,
                sort == EsItemSort.HIGHEST_SELL_PRICE_FIRST,
                EsItem::bestSellCurrency.name,
                EsItem::bestSellAmount.name,
                blockchains,
            )
            EsItemSort.HIGHEST_BID_PRICE_FIRST,
            EsItemSort.LOWEST_BID_PRICE_FIRST -> esEntityQueryScoreService.buildQuery(
                boolQuery,
                sort == EsItemSort.HIGHEST_BID_PRICE_FIRST,
                EsItem::bestBidCurrency.name,
                EsItem::bestBidAmount.name,
                blockchains,
            )
        }
    }
}
