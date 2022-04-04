package com.rarible.protocol.union.search.core.service.query

import com.rarible.protocol.union.search.core.ElasticActivity
import com.rarible.protocol.union.search.core.filter.ActivitySort
import com.rarible.protocol.union.search.core.filter.ElasticActivityFilter
import com.rarible.protocol.union.search.core.filter.ElasticActivityQueryGenericFilter
import com.rarible.protocol.union.search.core.filter.ElasticActivityQueryPerTypeFilter
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.TermsQueryBuilder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Service

@Service
class QueryBuilderService(
    private val sortService: QuerySortService,
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
        when (filter) {
            is ElasticActivityQueryGenericFilter -> builder.applyGenericFilter(filter)
            is ElasticActivityQueryPerTypeFilter -> builder.applyPerTypeFilter(filter)
        }
        sortService.applySort(builder, sort)

        return builder.build()
    }

    private fun NativeSearchQueryBuilder.applyGenericFilter(filter: ElasticActivityQueryGenericFilter) {
        val queryBuilder = BoolQueryBuilder()

        queryBuilder.mustMatchTerms(filter.blockchains, ElasticActivity::blockchain.name)
        queryBuilder.mustMatchTerms(filter.activityTypes, ElasticActivity::type.name)
        queryBuilder.anyMustMatchTerms(filter.anyUsers, userMaker, userTaker)
        queryBuilder.mustMatchTerms(filter.makers, userMaker)
        queryBuilder.mustMatchTerms(filter.takers, userTaker)
        queryBuilder.anyMustMatchTerms(filter.anyCollections, collectionMake, collectionTake)
        queryBuilder.mustMatchTerms(filter.makeCollections, collectionMake)
        queryBuilder.mustMatchTerms(filter.takeCollections, collectionTake)
        queryBuilder.anyMustMatchTerms(filter.anyItems, itemMake, itemTake)
        queryBuilder.mustMatchTerms(filter.makeItems, itemMake)
        queryBuilder.mustMatchTerms(filter.takeItems, itemTake)

        if (filter.from != null || filter.to != null) {
            val rangeQueryBuilder = RangeQueryBuilder(ElasticActivity::date.name)
            if (filter.from != null) {
                rangeQueryBuilder.gte(filter.from)
            }
            if (filter.to != null) {
                rangeQueryBuilder.lte(filter.to)
            }
            queryBuilder.must(rangeQueryBuilder)
        }

        withQuery(queryBuilder)
    }

    private fun NativeSearchQueryBuilder.applyPerTypeFilter(filter: ElasticActivityQueryPerTypeFilter) {
        TODO("To be implemented under ALPHA-276 Epic")
    }

    private fun BoolQueryBuilder.mustMatchTerms(terms: Set<*>, field: String) {
        if (terms.isNotEmpty()) {
            must(TermsQueryBuilder(field, prepareTerms(terms)))
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
        return terms.map { it.toString().lowercase() }
    }
}
