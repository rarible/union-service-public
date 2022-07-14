package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemFilter
import com.rarible.protocol.union.core.model.EsItemGenericFilter
import com.rarible.protocol.union.core.model.EsItemSort
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MultiMatchQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.rangeQuery
import org.elasticsearch.index.query.QueryBuilders.termsQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsItemQueryBuilderService(
    private val cursorService: EsItemQueryCursorService,
    private val sortService: EsItemQuerySortService,
) {

    fun build(filter: EsItemFilter, sort: EsItemSort): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val query = BoolQueryBuilder()
        when (filter) {
            is EsItemGenericFilter -> query.applyGenericFilter(filter)
        }

        sortService.applySort(builder, sort)
        cursorService.applyCursor(query, sort, filter.cursor)

        builder.withQuery(query)
        return builder.build()
    }

    private fun BoolQueryBuilder.applyGenericFilter(filter: EsItemGenericFilter) {
        mustMatchTerms(filter.collections, EsItem::collection.name)
        mustMatchTerms(filter.creators, EsItem::creators.name)
        mustMatchTerms(filter.blockchains, EsItem::blockchain.name)
        mustMatchTerms(filter.itemIds, EsItem::itemId.name)
        mustMatchRange(filter.mintedFrom, filter.mintedTo, EsItem::mintedAt.name)
        applyTextFilter(filter.text)
        applyTraitsKeysFilter(filter.traitsKeys)
        applyTraitsValuesFilter(filter.traitsValues)
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

            val trimmedText = text!!.trim()
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

    private fun BoolQueryBuilder.applyTraitsKeysFilter(traitsKeys: List<String?>) {
        if (traitsKeys != null) {
            query.should(
                QueryBuilders.nestedQuery(
                    "traits",
                    QueryBuilders.boolQuery().must(termsQuery("traits.key.raw", traitsKeys)),
                    ScoreMode.None
                )
            )
        }
    }

    private fun BoolQueryBuilder.applyTraitsValuesFilter(traitsValues: List<String>?) {
        if (traitsValues != null) {
            query.should(
                QueryBuilders.nestedQuery(
                    "traits",
                    QueryBuilders.boolQuery().must(termsQuery("traits.value.raw", traitsValues)),
                    ScoreMode.None
                )
            )
        }
    }
}
