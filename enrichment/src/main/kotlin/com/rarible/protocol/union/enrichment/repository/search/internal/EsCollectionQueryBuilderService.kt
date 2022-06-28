package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.model.EsCollectionCursor
import com.rarible.protocol.union.core.model.EsCollectionFilter
import com.rarible.protocol.union.core.model.EsCollectionGenericFilter
import com.rarible.protocol.union.core.model.EsOwnership
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
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

        builder.sortByField(EsCollection::date.name, SortOrder.DESC)
        builder.sortByField(EsCollection::salt.name, SortOrder.DESC)

        builder.withQuery(query)
        return builder.build()
    }

    private fun BoolQueryBuilder.applyGenericFilter(filter: EsCollectionGenericFilter) {
        mustMatchTerms(filter.blockchains, EsCollection::blockchain.name)
        mustMatchTerms(filter.owners, EsCollection::owner.name)
    }
}
