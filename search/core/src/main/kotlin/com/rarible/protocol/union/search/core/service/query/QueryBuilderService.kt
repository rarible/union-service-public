package com.rarible.protocol.union.search.core.service.query

import com.rarible.protocol.union.search.core.ElasticActivity
import com.rarible.protocol.union.search.core.model.ActivitySort
import com.rarible.protocol.union.search.core.model.ElasticActivityFilter
import com.rarible.protocol.union.search.core.model.ElasticActivityQueryGenericFilter
import com.rarible.protocol.union.search.core.model.ElasticActivityQueryPerTypeFilter
import com.rarible.protocol.union.search.core.model.cursor
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.TermsQueryBuilder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Service

@Service
class QueryBuilderService(
    private val sortService: QuerySortService,
    private val cursorService: QueryCursorService,
) {

    companion object {
        private val userMaker = ElasticActivity::user.name + "." + ElasticActivity.User::maker.name
        private val userTaker = ElasticActivity::user.name + "." + ElasticActivity.User::taker.name
        private val collectionMake = ElasticActivity::collection.name + "." + ElasticActivity.Collection::make.name
        private val collectionTake = ElasticActivity::collection.name + "." + ElasticActivity.Collection::take.name
        private val itemMake = ElasticActivity::item.name + "." + ElasticActivity.Item::make.name
        private val itemTake = ElasticActivity::item.name + "." + ElasticActivity.Item::take.name
    }

    fun build(filter: ElasticActivityFilter, sort: ActivitySort): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val query = BoolQueryBuilder()
        when (filter) {
            is ElasticActivityQueryGenericFilter -> query.applyGenericFilter(filter)
            is ElasticActivityQueryPerTypeFilter -> query.applyPerTypeFilter(filter)
        }
        sortService.applySort(builder, sort)
        cursorService.applyCursor(query, sort, filter.cursor)

        builder.withQuery(query)
        return builder.build()
    }

    private fun BoolQueryBuilder.applyGenericFilter(filter: ElasticActivityQueryGenericFilter) {
        mustMatchTerms(filter.blockchains, ElasticActivity::blockchain.name)
        mustMatchTerms(filter.activityTypes, ElasticActivity::type.name)
        anyMustMatchTerms(filter.anyUsers, userMaker, userTaker)
        mustMatchTerms(filter.makers, userMaker)
        mustMatchTerms(filter.takers, userTaker)
        anyMustMatchTerms(filter.anyCollections, collectionMake, collectionTake)
        mustMatchTerms(filter.makeCollections, collectionMake)
        mustMatchTerms(filter.takeCollections, collectionTake)
        anyMustMatchKeyword(filter.anyItem, itemMake, itemTake)
        mustMatchKeyword(filter.makeItem, itemMake)
        mustMatchKeyword(filter.takeItem, itemTake)

        if (filter.from != null || filter.to != null) {
            val rangeQueryBuilder = RangeQueryBuilder(ElasticActivity::date.name)
            if (filter.from != null) {
                rangeQueryBuilder.gte(filter.from)
            }
            if (filter.to != null) {
                rangeQueryBuilder.lte(filter.to)
            }
            must(rangeQueryBuilder)
        }
    }

    private fun BoolQueryBuilder.applyPerTypeFilter(filter: ElasticActivityQueryPerTypeFilter) {
        TODO("To be implemented under ALPHA-276 Epic")
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

    private fun BoolQueryBuilder.anyMustMatchKeyword(keyword: String?, vararg fields: String) {
        if (!keyword.isNullOrEmpty()) {
            val boolQueryBuilder = BoolQueryBuilder()
            fields.forEach {
                boolQueryBuilder.should(MatchQueryBuilder(it, keyword))
            }
            boolQueryBuilder.minimumShouldMatch(1)
            must(boolQueryBuilder)
        }
    }

    private fun prepareTerms(terms: Set<*>): List<String> {
        return terms.map { it.toString().lowercase() }
    }
}
