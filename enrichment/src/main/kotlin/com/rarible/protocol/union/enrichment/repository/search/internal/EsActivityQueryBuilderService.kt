package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.elastic.ElasticActivityFilter
import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivitySort
import com.rarible.protocol.union.dto.ActivityTypeDto
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
        private val currency = EsActivity::currency.name
        private val item = EsActivity::item.name
        private val blockchain = EsActivity::blockchain.name
        private val type = EsActivity::type.name
        private val bidActivities = listOf(ActivityTypeDto.BID.name, ActivityTypeDto.CANCEL_BID.name)
    }

    fun build(filter: ElasticActivityFilter, sort: EsActivitySort): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val queryFilter = BoolQueryBuilder()
        queryFilter.mustMatchTerms(filter.blockchains, blockchain)
        queryFilter.mustMatchTerms(filter.activityTypes, type)
        queryFilter.anyMustMatchTerms(filter.anyUsers, userFrom, userTo)
        queryFilter.mustMatchTerms(filter.usersFrom, userFrom)
        queryFilter.mustMatchTerms(filter.usersTo, userTo)
        if (filter.collections.isNotEmpty()) {
            val collectionQuery = QueryBuilders.boolQuery()
            filter.collections.forEach { (blockchain, address) ->
                collectionQuery.should(
                    matchCollection(blockchain, address)
                )
            }
            queryFilter.must(collectionQuery)
        }
        if (filter.bidCurrencies.isNotEmpty()) {
            val currenciesQuery = QueryBuilders.boolQuery()
            filter.bidCurrencies.forEach { (blockchain, value) ->
                currenciesQuery.should(
                    matchCurrency(blockchain, value)
                )
            }
            queryFilter.must(currenciesQuery)
        }
        queryFilter.mustMatchKeyword(filter.item, item)

        if (filter.from != null || filter.to != null) {
            val rangeQueryBuilder = RangeQueryBuilder(EsActivity::date.name)
            if (filter.from != null) {
                rangeQueryBuilder.gte(filter.from)
            }
            if (filter.to != null) {
                rangeQueryBuilder.lte(filter.to)
            }
            queryFilter.must(rangeQueryBuilder)
        }
        val query = QueryBuilders.boolQuery().filter(queryFilter)
        sortService.applySort(builder, sort)
        cursorService.applyCursor(queryFilter, sort, filter.cursor)

        builder.withQuery(query)
        return builder.build()
    }

    private fun matchCurrency(
        blockchain: BlockchainDto,
        value: String
    ) = QueryBuilders
        .boolQuery()
        .should(
            QueryBuilders.boolQuery().must(
                QueryBuilders.termQuery(EsActivityQueryBuilderService.blockchain, blockchain.name)
            ).must(
                QueryBuilders.termQuery(currency, value)
            ).must(QueryBuilders.termsQuery(type, bidActivities))
        ).should(
            QueryBuilders.boolQuery().mustNot(
                QueryBuilders.termsQuery(type, bidActivities)
            )
        )

    private fun matchCollection(
        blockchain: BlockchainDto,
        address: String
    ) = QueryBuilders
        .boolQuery()
        .must(
            QueryBuilders.termQuery(EsActivityQueryBuilderService.blockchain, blockchain.name)
        ).must(
            QueryBuilders.termQuery(collection, address)
        )

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
