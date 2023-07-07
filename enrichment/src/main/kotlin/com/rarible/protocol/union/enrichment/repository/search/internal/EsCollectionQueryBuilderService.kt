package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.elastic.EsCollection
import com.rarible.protocol.union.core.model.elastic.EsCollectionFilter
import com.rarible.protocol.union.core.model.elastic.EsCollectionGenericFilter
import com.rarible.protocol.union.core.model.elastic.EsCollectionTextFilter
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsCollectionQueryBuilderService(
    private val cursorService: EsCollectionQueryCursorService,
) : AbstractQueryBuilderService() {

    override fun textFieldsWithBoost(): Map<String, Float> {
        return mapOf(
            "name" to boost(100),
            "name.camelCase" to boost(100),
            "name.join" to boost(100),
            "name.initials" to boost(100),
            "name.keyword" to boost(100),
            "name.specialCharacter" to boost(100),

            "meta.name" to boost(100),
            "meta.name.camelCase" to boost(100),
            "meta.name.join" to boost(100),
            "meta.name.initials" to boost(100),
            "meta.name.keyword" to boost(100),
            "meta.name.specialCharacter" to boost(100),
        )
    }

    override fun keywordFieldsWithBoost(): Map<String, Float> {
        return emptyMap()
    }

    fun build(filter: EsCollectionFilter): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val query = BoolQueryBuilder()
        when (filter) {
            is EsCollectionGenericFilter -> query.applyGenericFilter(filter)
            is EsCollectionTextFilter -> query.applyTestSearchFilter(filter)
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

    private fun BoolQueryBuilder.applyTestSearchFilter(filter: EsCollectionTextFilter) {
        if (filter.blockchains.isNotEmpty()) {
            mustMatchTerms(filter.blockchains, EsCollection::blockchain.name)
        }
        applyFullTextQuery(filter.text)
    }
}
