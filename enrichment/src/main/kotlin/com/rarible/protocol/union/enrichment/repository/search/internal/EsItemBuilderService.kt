package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.ElasticItemFilter
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemQueryCursorService.applyCursor
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemQuerySortService.applySort
import org.elasticsearch.index.query.BoolQueryBuilder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder

object EsItemBuilderService {

    fun ElasticItemFilter.buildQuery(sort: EsItemSort): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val query = BoolQueryBuilder()
        query.applyCursor(sort, this.cursor)
        builder.withQuery(query)
        builder.applySort(sort)
        return builder.build()
    }
}
