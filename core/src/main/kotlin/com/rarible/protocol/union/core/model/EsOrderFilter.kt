package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.data.elasticsearch.core.query.Query

interface EsOrderFilter {

    fun asQuery(): Query

    fun genericBuild(
        continuation: DateIdContinuation?,
        size: Int,
        sort: OrderSortDto,
        vararg queryBuilders: QueryBuilder,
    ): Query {
        val continuationQuery = continuation?.let {
            QueryBuilders.boolQuery()
                .should(
                    QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(EsOrder::lastUpdatedAt.name, it.date))
                        .must(QueryBuilders.rangeQuery(EsOrder::orderId.name).lt(it.id))
                )
                .should(
                    QueryBuilders.rangeQuery(EsOrder::lastUpdatedAt.name).lt(it.date)
                )
        }
        val builders = listOfNotNull(*queryBuilders, continuationQuery)
        val fullQueryBuilder = builders.singleOrNull()
            ?: QueryBuilders.boolQuery().also { builders.forEach(it::must) }

        val sortOrder = if (sort == OrderSortDto.LAST_UPDATE_DESC) SortOrder.DESC else SortOrder.ASC
        return NativeSearchQueryBuilder()
            .withQuery(fullQueryBuilder)
            .withSort(SortBuilders.fieldSort(EsOrder::lastUpdatedAt.name).order(sortOrder))
            .withSort(SortBuilders.fieldSort(EsOrder::orderId.name).order(sortOrder))
            .withMaxResults(size)
            .build()
    }
}

@ExperimentalStdlibApi
data class EsAllOrderFilter(
    private val blockchains: Collection<BlockchainDto>?,
    private val continuation: DateIdContinuation?,
    private val size: Int,
    private val sort: OrderSortDto,
    private val status: List<OrderStatusDto>?
) : EsOrderFilter {

    override fun asQuery(): Query {

        val list = buildList {
            if (blockchains != null) {
                add(QueryBuilders.termsQuery(EsOrder::blockchain.name, blockchains.map { it.name }))
            }
            if (status != null) {
                add(QueryBuilders.termsQuery(EsOrder::status.name, status.map { it.name }))
            }
        }

        return genericBuild(continuation, size, sort, *list.toTypedArray())
    }
}

data class EsOrderByIdFilter(
    val orderId: String,
) : EsOrderFilter {
    override fun asQuery(): Query =
        NativeSearchQueryBuilder().withQuery(QueryBuilders.idsQuery().addIds(orderId)).build()
}

data class EsOrderByIdsFilter(
    val orderIds: Collection<String>,
) : EsOrderFilter {
    override fun asQuery(): Query =
        NativeSearchQueryBuilder().withQuery(QueryBuilders.idsQuery().addIds(*orderIds.toTypedArray())).build()
}