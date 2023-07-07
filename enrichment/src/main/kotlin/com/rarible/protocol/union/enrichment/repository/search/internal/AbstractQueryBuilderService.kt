package com.rarible.protocol.union.enrichment.repository.search.internal

import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MultiMatchQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import kotlin.math.pow

abstract class AbstractQueryBuilderService {
    protected abstract fun textFieldsWithBoost(): Map<String, Float>

    protected abstract fun keywordFieldsWithBoost(): Map<String, Float>

    protected fun BoolQueryBuilder.applyFullTextQuery(text: String?): BoolQueryBuilder {
        val textAndKeywordsFields = textFieldsWithBoost() + keywordFieldsWithBoost()
        fullTextClauses(this, text, textAndKeywordsFields)
        return this.minimumShouldMatch(1)
    }

    private fun fullTextClauses(
        boolQuery: BoolQueryBuilder,
        text: String?,
        fields: Map<String, Float>
    ) {
        if (text.isNullOrBlank()) return
        // simpleQueryString. boost = 1
        val trimmedText = text.trim()
        val lastTerm = trimmedText.split(" ").last()
        val textForSearch = if (lastTerm == trimmedText) {
            "($lastTerm | $lastTerm*)"
        } else {
            trimmedText.replaceAfterLast(" ", "($lastTerm | $lastTerm*)")
        }
        val joinedText = trimmedText.replace("\\s*".toRegex(), "")
        val joinedTextForSearch = "(${QueryParserUtil.escape(joinedText)} | ${QueryParserUtil.escape(joinedText)}*)"
        boolQuery.should(
            QueryBuilders.simpleQueryStringQuery(textForSearch)
                .defaultOperator(Operator.AND)
                .fuzzyTranspositions(false)
                .fuzzyMaxExpansions(0)
                .fields(fields)
        )
        boolQuery.should(
            QueryBuilders.simpleQueryStringQuery(joinedTextForSearch)
                .defaultOperator(Operator.AND)
                .fuzzyTranspositions(false)
                .fuzzyMaxExpansions(0)
                .fields(fields)
        )
        boolQuery.should(
            QueryBuilders.multiMatchQuery(text)
                .fields(fields)
                .fuzzyTranspositions(false)
                .operator(Operator.AND)
                .type(MultiMatchQueryBuilder.Type.PHRASE)
        )
    }

    protected fun boost(x: Int): Float = x * 10f.pow(x.toFloat() / 4f)
}
