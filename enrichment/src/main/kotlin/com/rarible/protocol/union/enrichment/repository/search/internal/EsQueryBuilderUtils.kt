package com.rarible.protocol.union.enrichment.repository.search.internal

import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.TermsQueryBuilder

fun BoolQueryBuilder.mustMatchTerms(terms: Set<*>, field: String) {
    if (terms.isNotEmpty()) {
        must(TermsQueryBuilder(field, prepareTerms(terms)))
    }
}

fun prepareTerms(terms: Set<*>): List<String> {
    return terms.map { it.toString() /* .lowercase() */ }
}