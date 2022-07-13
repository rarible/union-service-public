package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.ElasticItemFilter
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemQueryCursorService.applyCursor
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemQuerySortService.applySort
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MultiMatchQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.rangeQuery
import org.elasticsearch.index.query.QueryBuilders.termsQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder

object EsItemBuilderService {

    fun ElasticItemFilter.buildQuery(sort: EsItemSort): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val query = BoolQueryBuilder()

        if (!collections.isNullOrEmpty()) {
            query.must(termsQuery(EsItem::collection.name, collections))
        }
        if (!creators.isNullOrEmpty()) {
            query.must(termsQuery(EsItem::creators.name, creators))
        }
        if (!blockchains.isNullOrEmpty()) {
            query.must(termsQuery(EsItem::blockchain.name, blockchains))
        }
        if (!itemIds.isNullOrEmpty()) {
            query.must(termsQuery(EsItem::itemId.name, itemIds))
        }
        if (mintedFrom != null || mintedTo != null) {
            query.must(rangeQuery(EsItem::mintedAt.name).gte(mintedFrom).lte(mintedTo))
        }
        if (updatedFrom != null || updatedTo != null) {
            query.must(rangeQuery(EsItem::lastUpdatedAt.name).gte(updatedFrom).lte(updatedTo))
        }

        if (traitsKeys != null) {
            query.should(
                QueryBuilders.nestedQuery(
                    "traits",
                    QueryBuilders.boolQuery().must(termsQuery("traits.key.raw", traitsKeys)),
                    ScoreMode.None
                )
            )
        }

        if (traitsValues != null) {
            query.should(
                QueryBuilders.nestedQuery(
                    "traits",
                    QueryBuilders.boolQuery().must(termsQuery("traits.value.raw", traitsValues)),
                    ScoreMode.None
                )
            )
        }

        if (!text.isNullOrBlank()) {
            query.should(
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
            query.should(
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

            query.minimumShouldMatch(1)
        }

        query.applyCursor(sort, this.cursor)
        builder.withQuery(query)
        builder.applySort(sort)
        return builder.build()
    }
}
