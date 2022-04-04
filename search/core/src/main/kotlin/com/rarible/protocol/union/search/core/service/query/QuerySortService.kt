package com.rarible.protocol.union.search.core.service.query

import com.rarible.protocol.union.search.core.ElasticActivity
import com.rarible.protocol.union.search.core.filter.ActivitySort
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Service

@Service
class QuerySortService {

    fun applySort(builder: NativeSearchQueryBuilder, sort: ActivitySort) {
        val sortOrder = if (sort.latestFirst) SortOrder.DESC else SortOrder.ASC
        builder.sortByField(ElasticActivity::date.name, sortOrder)
        builder.sortByField(ElasticActivity::blockNumber.name, sortOrder)
        builder.sortByField(ElasticActivity::logIndex.name, sortOrder)
    }

    private fun NativeSearchQueryBuilder.sortByField(fieldName: String, order: SortOrder) {
        val sort = SortBuilders
            .fieldSort(fieldName)
            .order(order)
        withSort(sort)
    }
}