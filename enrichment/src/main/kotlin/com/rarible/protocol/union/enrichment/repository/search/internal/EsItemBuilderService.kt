package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.ElasticItemFilter
import com.rarible.protocol.union.core.model.EsItemSort
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsItemBuilderService(private val esItemQuerySortService: EsItemQuerySortService) {

    fun build(filter: ElasticItemFilter, sort: EsItemSort): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val query = SearchSourceBuilder().query(BoolQueryBuilder())
        query.searchAfter(arrayOf(filter.cursor))
        builder.withQuery(query.query())
        esItemQuerySortService.applySort(builder, sort)
        return builder.build()
    }

    fun build2(filter: ElasticItemFilter): BoolQueryBuilder {
        val query = BoolQueryBuilder()
        return query
    }
}
