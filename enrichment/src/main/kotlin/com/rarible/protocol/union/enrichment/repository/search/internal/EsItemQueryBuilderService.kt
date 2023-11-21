package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsItemFilter
import com.rarible.protocol.union.core.model.elastic.EsItemGenericFilter
import com.rarible.protocol.union.core.model.elastic.EsItemSort
import com.rarible.protocol.union.core.model.elastic.EsItemSortType
import com.rarible.protocol.union.core.model.elastic.FullTextSearch
import com.rarible.protocol.union.core.model.elastic.TextField
import com.rarible.protocol.union.core.model.elastic.TraitFilter
import com.rarible.protocol.union.core.model.elastic.TraitRangeFilter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.SearchProperties
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
    private val searchProperties: SearchProperties,
) {
    companion object {
        private val SCORE_SORT_TYPES: Set<EsItemSortType> = setOf(
            EsItemSortType.LOWEST_SELL_PRICE_FIRST,
            EsItemSortType.HIGHEST_SELL_PRICE_FIRST,
            EsItemSortType.LOWEST_BID_PRICE_FIRST,
            EsItemSortType.HIGHEST_BID_PRICE_FIRST
        )
    }

    suspend fun build(filter: EsItemFilter, sort: EsItemSort): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val isScoreSort = SCORE_SORT_TYPES.contains(sort.type)

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
        mustExists(filter.onSale, EsItem::bestSellAmount.name)
        applyFullTextSearch(filter.fullText)
        applyNamesTextFilter(filter.names)
        applyTraitsFilter(filter.traits)
        applyTraitRangesFilter(filter.traitRanges)
    }

    private fun BoolQueryBuilder.applyFullTextSearch(text: FullTextSearch?) {
        if (text == null || text.text.isBlank()) {
            return
        }
        val targetFields = fieldsWithBoost(text.fields)
        applyFullTextSearch(text.text, targetFields)
    }

    private fun fieldsWithBoost(fields: List<TextField>): Map<String, Float> =
        (fields.map { it.esField } + listOf("token", "tokenId"))
            .associateWith { searchProperties.item[it] ?: 1.0f }

    private fun BoolQueryBuilder.applyFullTextSearch(text: String, fields: Map<String, Float>) {
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
                .fields(fields)
        ).should( // phrase. boost = 100
            QueryBuilders.multiMatchQuery(text)
                .fields(fields)
                .boost(100f)
                .fuzzyTranspositions(false)
                .operator(Operator.AND)
                .type(MultiMatchQueryBuilder.Type.PHRASE)
        )
        minimumShouldMatch(1)
    }

    private fun BoolQueryBuilder.applyNamesTextFilter(names: Set<String>?) {
        if (names.isNullOrEmpty()) {
            return
        }
        for (name in names) {
            val trimmedText = name.trim()
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
                    .analyzeWildcard(true)
            )
        }
        minimumShouldMatch(1)
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

    private fun BoolQueryBuilder.applyTraitRangesFilter(traits: List<TraitRangeFilter>?) {
        traits?.filter { it.valueRange.from != null || it.valueRange.to != null }?.forEach {
            val rangeQuery = QueryBuilders.rangeQuery("traits.value.numeric")
            it.valueRange.from?.let { rangeQuery.gte(it) }
            it.valueRange.to?.let { rangeQuery.lte(it) }
            must(
                QueryBuilders.nestedQuery(
                    "traits",
                    QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery("traits.key.raw", it.key))
                        .must(rangeQuery),
                    ScoreMode.None
                )
            )
        }
    }
}
