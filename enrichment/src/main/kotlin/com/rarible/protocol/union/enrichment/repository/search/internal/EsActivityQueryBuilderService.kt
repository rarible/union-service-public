package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.ActivityByCollectionFilter
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.core.model.ElasticActivityFilter
import com.rarible.protocol.union.core.model.ElasticActivityQueryGenericFilter
import com.rarible.protocol.union.dto.BlockchainDto
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.TermsQueryBuilder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsActivityQueryBuilderService(
    private val sortService: EsActivityQuerySortService,
    private val cursorService: EsActivityQueryCursorService,
) {

    companion object {
        private val userFrom = EsActivity::userFrom.name
        private val userTo = EsActivity::userTo.name
        private val collection = EsActivity::collection.name
        private val item = EsActivity::item.name
    }

    fun build(filter: ElasticActivityFilter, sort: EsActivitySort): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val query = BoolQueryBuilder()
        when (filter) {
            is ElasticActivityQueryGenericFilter -> query.applyGenericFilter(filter)
            is ActivityByCollectionFilter -> query.applyByCollectionFilter(filter)
        }
        sortService.applySort(builder, sort)
        cursorService.applyCursor(query, sort, filter.cursor)

        builder.withQuery(query)
        return builder.build()
    }

    private fun BoolQueryBuilder.applyByCollectionFilter(filter: ActivityByCollectionFilter) {
        val collectionQuery = QueryBuilders.boolQuery()

        filter.collections.forEach { (blockchain, address) ->
            collectionQuery.should(
                matchCollection(blockchain, address)
            )
        }
        must(collectionQuery)

        mustMatchTerms(
            filter.activityTypes.map { it.name }.toSet(),
            EsActivity::type.name
        )
    }

    private fun matchCollection(
        blockchain: BlockchainDto,
        address: String
    ) = QueryBuilders
        .boolQuery()
        .must(
            QueryBuilders.termQuery(EsActivity::blockchain.name, blockchain.name)
        ).must(
            QueryBuilders.termQuery(EsActivity::collection.name, address)
        )

    private fun BoolQueryBuilder.applyGenericFilter(filter: ElasticActivityQueryGenericFilter) {
        mustMatchTerms(filter.blockchains, EsActivity::blockchain.name)
        mustMatchTerms(filter.activityTypes, EsActivity::type.name)
        anyMustMatchTerms(filter.anyUsers, userFrom, userTo)
        mustMatchTerms(filter.usersFrom, userFrom)
        mustMatchTerms(filter.usersTo, userTo)
        mustMatchTerms(filter.collections, collection)
        mustMatchKeyword(filter.item, item)

        if (filter.from != null || filter.to != null) {
            val rangeQueryBuilder = RangeQueryBuilder(EsActivity::date.name)
            if (filter.from != null) {
                rangeQueryBuilder.gte(filter.from)
            }
            if (filter.to != null) {
                rangeQueryBuilder.lte(filter.to)
            }
            must(rangeQueryBuilder)
        }
    }

    private fun BoolQueryBuilder.mustMatchTerms(terms: Set<*>, field: String) {
        if (terms.isNotEmpty()) {
            must(TermsQueryBuilder(field, prepareTerms(terms)))
        }
    }

    private fun BoolQueryBuilder.mustMatchKeyword(keyword: String?, field: String) {
        if (!keyword.isNullOrEmpty()) {
            must(MatchQueryBuilder(field, keyword))
        }
    }

    private fun BoolQueryBuilder.anyMustMatchTerms(terms: Set<*>, vararg fields: String) {
        if (terms.isNotEmpty()) {
            val boolQueryBuilder = BoolQueryBuilder()
            val preparedTerms = prepareTerms(terms)
            fields.forEach {
                boolQueryBuilder.should(TermsQueryBuilder(it, preparedTerms))
            }
            boolQueryBuilder.minimumShouldMatch(1)
            must(boolQueryBuilder)
        }
    }

    private fun prepareTerms(terms: Set<*>): List<String> {
        return terms.map { it.toString() }
    }
}
