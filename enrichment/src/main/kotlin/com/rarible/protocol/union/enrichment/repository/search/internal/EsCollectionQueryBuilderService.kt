package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.elastic.EsCollection
import com.rarible.protocol.union.core.model.elastic.EsCollectionFilter
import com.rarible.protocol.union.core.model.elastic.EsCollectionGenericFilter
import com.rarible.protocol.union.core.model.elastic.EsCollectionTextFilter
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.data.mapping.div
import org.springframework.stereotype.Component

@Component
class EsCollectionQueryBuilderService(
    private val cursorService: EsCollectionQueryCursorService,
    private val featureFlagsProperties: FeatureFlagsProperties,
) : AbstractQueryBuilderService(featureFlagsProperties) {

    override fun textFieldsWithBoost(): Map<String, Float> {
        return mapOf(
            EsCollection::name.name to boost(100),
            EsCollection::symbol.name to boost(100),
            (EsCollection::meta / EsCollection.CollectionMeta::name).name to boost(100),
            (EsCollection::meta / EsCollection.CollectionMeta::description).name to boost(100),
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
