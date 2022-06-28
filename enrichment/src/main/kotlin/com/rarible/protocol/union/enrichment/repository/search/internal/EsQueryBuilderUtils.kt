package com.rarible.protocol.union.enrichment.repository.search.internal

import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.TermQueryBuilder
import org.elasticsearch.index.query.TermsQueryBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder

fun BoolQueryBuilder.mustMatchTerms(terms: Set<*>, field: String) {
    if (terms.isNotEmpty()) {
        must(TermsQueryBuilder(field, prepareTerms(terms)))
    }
}

fun BoolQueryBuilder.mustMatchTerm(term: String?, field: String) {
    if (!term.isNullOrEmpty()) {
        must(TermQueryBuilder(field, term))
    }
}

fun prepareTerms(terms: Set<*>): List<String> {
    return terms.map { it.toString() /* .lowercase() */ }
}

fun NativeSearchQueryBuilder.sortByField(fieldName: String, order: SortOrder) {
    val sort = SortBuilders
        .fieldSort(fieldName)
        .order(order)
    withSort(sort)
}
