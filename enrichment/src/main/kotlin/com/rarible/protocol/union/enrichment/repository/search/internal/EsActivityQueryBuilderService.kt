package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.elastic.ElasticActivityFilter
import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivitySort
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.UnionBlockchainId
import org.elasticsearch.index.query.BoolQueryBuilder
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
        private val USER_FROM = EsActivity::userFrom.name
        private val USER_TO = EsActivity::userTo.name
        private val COLLECTION = EsActivity::collection.name
        private val CURRENCY = EsActivity::currency.name
        private val ITEM = EsActivity::item.name
        private val BLOCKCHAIN = EsActivity::blockchain.name
        private val TYPE = EsActivity::type.name

        private val bidActivities = listOf(ActivityTypeDto.BID.name, ActivityTypeDto.CANCEL_BID.name)
    }

    fun build(filter: ElasticActivityFilter, sort: EsActivitySort): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val queryFilter = BoolQueryBuilder()
        queryFilter.mustMatchTerms(filter.blockchains, BLOCKCHAIN)
        queryFilter.mustMatchTerms(filter.activityTypes, TYPE)
        queryFilter.anyMustMatchTerms(filter.anyUsers, USER_FROM, USER_TO)
        queryFilter.mustMatchTerms(filter.usersFrom, USER_FROM)
        queryFilter.mustMatchTerms(filter.usersTo, USER_TO)

        queryFilter.mustMatchFilters(filter.collections, COLLECTION, this::matchBlockchainId)
        queryFilter.mustMatchFilters(filter.items, ITEM, this::matchBlockchainId)
        queryFilter.mustMatchFilters(filter.bidCurrencies, CURRENCY, this::matchBidCurrency)

        if (filter.from != null || filter.to != null) {
            val rangeQueryBuilder = RangeQueryBuilder(EsActivity::date.name)
            filter.from?.let { rangeQueryBuilder.gte(it) }
            filter.to?.let { rangeQueryBuilder.lte(it) }
            queryFilter.must(rangeQueryBuilder)
        }

        val query = QueryBuilders.boolQuery().filter(queryFilter)
        sortService.applySort(builder, sort)
        cursorService.applyCursor(queryFilter, sort, filter.cursor)

        builder.withQuery(query)
        return builder.build()
    }

    private fun <T> BoolQueryBuilder.mustMatchFilters(
        values: Collection<T>,
        field: String,
        filter: (value: T, field: String) -> BoolQueryBuilder
    ) {
        values.ifEmpty { return }
        val query = QueryBuilders.boolQuery()
        values.forEach { query.should(filter(it, field)) }
        this.must(query)
    }

    private fun matchBidCurrency(
        id: CurrencyIdDto,
        field: String
    ) = QueryBuilders
        .boolQuery()
        .should(
            QueryBuilders
                .boolQuery()
                .must(QueryBuilders.termQuery(BLOCKCHAIN, id.blockchain.name))
                .must(QueryBuilders.termQuery(field, id.value))
                .must(QueryBuilders.termsQuery(TYPE, bidActivities))
        ).should(
            QueryBuilders
                .boolQuery()
                .mustNot(QueryBuilders.termsQuery(TYPE, bidActivities))
        )

    private fun matchBlockchainId(
        id: UnionBlockchainId,
        field: String
    ) = QueryBuilders
        .boolQuery()
        .must(QueryBuilders.termQuery(BLOCKCHAIN, id.blockchain))
        .must(QueryBuilders.termQuery(field, id.value))

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
        return terms.map { it.toString() }
    }
}
