package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsItemFilter
import com.rarible.protocol.union.core.model.elastic.EsItemGenericFilter
import com.rarible.protocol.union.core.model.elastic.EsItemSort
import com.rarible.protocol.union.core.model.elastic.TraitFilter
import com.rarible.protocol.union.dto.BlockchainDto
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MultiMatchQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.termsQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsItemQueryBuilderService(
    private val cursorService: EsEntitySearchAfterCursorService,
    private val scoreService: EsItemQueryScoreService,
    private val sortService: EsItemQuerySortService,
    private val priceFilterService: EsItemQueryPriceFilterService,
) {
    companion object {
        private val SCORE_SORT_TYPES: Set<EsItemSort> = setOf(
            EsItemSort.LOWEST_SELL_PRICE_FIRST,
            EsItemSort.HIGHEST_SELL_PRICE_FIRST,
            EsItemSort.LOWEST_BID_PRICE_FIRST,
            EsItemSort.HIGHEST_BID_PRICE_FIRST
        )
    }

    suspend fun build(filter: EsItemFilter, sort: EsItemSort): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val isScoreSort = SCORE_SORT_TYPES.contains(sort)

        val blockchains = if (filter.blockchains.isNullOrEmpty()) {
            BlockchainDto.values().toSet()
        } else {
            filter.blockchains!!.map { BlockchainDto.valueOf(it) }.toSet()
        }

        var query: QueryBuilder = BoolQueryBuilder()
        query as BoolQueryBuilder
        when (filter) {
            is EsItemGenericFilter -> {
                query.applyGenericFilter(filter)
                priceFilterService.applyPriceFilter(query, filter, blockchains)
            }
        }

        if (isScoreSort) {
            query = scoreService.buildQuery(query, sort, blockchains)
        }

        sortService.applySort(builder, sort)

        builder.withQuery(query)
        val resultQuery = builder.build()
        val cursor = cursorService.tryFixLegacyCursor(filter.cursor)
        resultQuery.searchAfter = cursorService.buildSearchAfterClause(cursor, 2)
        return resultQuery
    }

    private fun BoolQueryBuilder.applyGenericFilter(filter: EsItemGenericFilter) {
        mustMatchTerms(filter.collections, EsItem::collection.name)
        mustMatchTerms(filter.creators, EsItem::creators.name)
        mustMatchTerms(filter.blockchains, EsItem::blockchain.name)
        mustMatchTerms(filter.itemIds, EsItem::itemId.name)
        mustMatchTerms(filter.sellPlatforms, EsItem::bestSellMarketplace.name)
        mustMatchTerms(filter.bidPlatforms, EsItem::bestBidMarketplace.name)
        mustMatchRange(filter.mintedFrom, filter.mintedTo, EsItem::mintedAt.name)
        mustMatchRange(filter.updatedFrom, filter.updatedTo, EsItem::lastUpdatedAt.name)
        applyTextFilter(filter.text)
        applyTraitsFilter(filter.traits)
    }

    private fun BoolQueryBuilder.applyTextFilter(text: String?) {
        if (!text.isNullOrBlank()) {
            should(
                QueryBuilders.nestedQuery(
                    "traits",
                    QueryBuilders.boolQuery().must(QueryBuilders.termQuery("traits.value.raw", text)),
                    ScoreMode.None
                )
            )

            val trimmedText = text.trim()
            val lastTerm = trimmedText.split(" ").last()
            val textForSearch = if (lastTerm == trimmedText) {
                "($lastTerm | $lastTerm*)"
            } else {
                trimmedText.replaceAfterLast(" ", "($lastTerm | $lastTerm*)")
            }
            should(
                QueryBuilders.simpleQueryStringQuery(textForSearch)
                    .defaultOperator(Operator.AND)
                    .fuzzyTranspositions(false)
                    .fuzzyMaxExpansions(0)
                    .fields(mapOf(EsItem::name.name to 1.0f))
            )
                // phrase. boost = 100
                .should(
                    QueryBuilders.multiMatchQuery(text)
                        .fields(mapOf(EsItem::name.name to 1.0f))
                        .boost(100f)
                        .fuzzyTranspositions(false)
                        .operator(Operator.AND)
                        .type(MultiMatchQueryBuilder.Type.PHRASE)
                )

            minimumShouldMatch(1)
        }
    }

    private fun BoolQueryBuilder.applyTraitsFilter(traits: List<TraitFilter>?) {
        traits?.forEach {

            must(
                QueryBuilders.nestedQuery(
                    "traits",
                    QueryBuilders.boolQuery().must(termsQuery("traits.key.raw", it.key))
                        .must(termsQuery("traits.value.raw", it.value)),
                    ScoreMode.None
                )
            )
        }
    }
}
