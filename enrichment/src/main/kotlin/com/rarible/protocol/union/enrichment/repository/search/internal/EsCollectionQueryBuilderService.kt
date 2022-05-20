package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.model.EsCollectionFilter
import com.rarible.protocol.union.core.model.EsCollectionGenericFilter
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsCollectionQueryBuilderService {

    fun build(filter: EsCollectionFilter): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val query = BoolQueryBuilder()
        when (filter) {
            is EsCollectionGenericFilter -> query.applyGenericFilter(filter)
        }

        if (!filter.cursor.isNullOrEmpty()) {
            query.must(RangeQueryBuilder(EsCollection::collectionId.name).gt(filter.cursor))
        }

        builder.withQuery(query)
        return builder.build()
    }

    private fun BoolQueryBuilder.applyGenericFilter(filter: EsCollectionGenericFilter) {
        mustMatchTerms(filter.blockchains, EsCollection::blockchain.name)
        mustMatchTerms(filter.owners, EsCollection::owner.name)
    }
}
