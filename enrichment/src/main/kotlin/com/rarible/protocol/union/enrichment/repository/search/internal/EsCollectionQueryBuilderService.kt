package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.elastic.EsCollection
import com.rarible.protocol.union.core.model.elastic.EsCollectionFilter
import com.rarible.protocol.union.core.model.elastic.EsCollectionGenericFilter
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsCollectionQueryBuilderService(
    private val cursorService: EsCollectionQueryCursorService
) {

    fun build(filter: EsCollectionFilter): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val query = BoolQueryBuilder()
        when (filter) {
            is EsCollectionGenericFilter -> query.applyGenericFilter(filter)
        }

        cursorService.applyCursor(query, filter.cursor)

        builder.sortByField(EsCollection::date, SortOrder.DESC)
        builder.sortByField(EsCollection::salt, SortOrder.DESC)

        builder.withQuery(query)
        return builder.build()
    }

    private fun BoolQueryBuilder.applyGenericFilter(filter: EsCollectionGenericFilter) {
        mustMatchTerms(filter.blockchains, EsCollection::blockchain.name)
        mustMatchTerms(filter.owners, EsCollection::owner.name)
    }
}
