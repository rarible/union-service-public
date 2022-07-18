package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.EsOwnershipByItemFilter
import com.rarible.protocol.union.core.model.EsOwnershipByOwnerFilter
import com.rarible.protocol.union.core.model.EsOwnershipFilter
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsOwnershipQueryBuilderService(
    private val cursorService: EsOwnershipQueryCursorService
) {

    fun build(filter: EsOwnershipFilter): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val query = BoolQueryBuilder()

        when (filter) {
            is EsOwnershipByItemFilter -> query.applyByItemFilter(filter)
            is EsOwnershipByOwnerFilter -> query.applyByOwnerFilter(filter)
        }

        cursorService.applyCursor(query, filter.cursor)

        builder.sortByField(EsOwnership::date, SortOrder.DESC)
        builder.sortByField(EsOwnership::ownershipId, SortOrder.DESC)

        builder.withQuery(query)
        return builder.build()
    }

    private fun BoolQueryBuilder.applyByItemFilter(filter: EsOwnershipByItemFilter) {
        mustMatchTerm(filter.itemId.fullId(), EsOwnership::itemId.name)
    }

    private fun BoolQueryBuilder.applyByOwnerFilter(filter: EsOwnershipByOwnerFilter) {
        mustMatchTerm(filter.owner.fullId(), EsOwnership::owner.name)
        mustMatchTerms(filter.blockchains?.toSet().orEmpty(), EsOwnership::blockchain.name)
    }
}
