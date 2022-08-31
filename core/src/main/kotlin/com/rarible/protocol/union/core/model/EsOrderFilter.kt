@file:OptIn(ExperimentalStdlibApi::class, ExperimentalStdlibApi::class)

package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.boolQuery
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
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

        val fullQueryBuilder = queryBuilders.singleOrNull()
            ?: QueryBuilders.boolQuery().also { queryBuilders.forEach(it::must) }

        val sortOrder = if (sort == OrderSortDto.LAST_UPDATE_DESC) SortOrder.DESC else SortOrder.ASC
        val searchQueryBuilder: NativeSearchQuery = NativeSearchQueryBuilder()
            .withQuery(fullQueryBuilder)
            .withSort(SortBuilders.fieldSort(EsOrder::lastUpdatedAt.name).order(sortOrder))
            .withSort(SortBuilders.fieldSort(EsOrder::orderId.name).order(sortOrder))
            .withMaxResults(size)
            .build()
        continuation?.run {
            searchQueryBuilder.searchAfter = listOf(date.toEpochMilli(), id)
        }

        return searchQueryBuilder
    }
}

data class EsAllOrderFilter(
    private val blockchains: Collection<BlockchainDto>?,
    private val cursor: DateIdContinuation?,
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

        return genericBuild(cursor, size, sort, *list.toTypedArray())
    }
}

data class EsOrderSellOrdersByItem(
    val itemId: String,
    val platform: PlatformDto?,
    val maker: String?,
    val origin: String?,
    val status: List<OrderStatusDto>?,
    val continuation: DateIdContinuation?,
    val sort: OrderSortDto,
    val size: Int
) : EsOrderFilter {
    override fun asQuery(): Query {
        val list = buildList {

            val (token, tokenId) = CompositeItemIdParser.splitWithBlockchain(itemId)
            add(
                boolQuery()
                    .must(
                        QueryBuilders.termsQuery(
                            "${EsOrder::make.name}.${EsOrder.Asset::token.name}",
                            token
                        )
                    )
                    .should(
                        QueryBuilders.termsQuery(
                            "${EsOrder::make.name}.${EsOrder.Asset::tokenId.name}",
                            tokenId
                        )
                    )
                    .should(
                        boolQuery().must(
                            QueryBuilders.termsQuery(
                                "${EsOrder::make.name}.${EsOrder.Asset::isNft.name}",
                                true
                            )
                        ).mustNot(
                            QueryBuilders.existsQuery(
                                "${EsOrder::make.name}.${EsOrder.Asset::tokenId.name}"
                            )
                        )
                    )

            )

            add(
                QueryBuilders.termsQuery(
                    EsOrder::type.name,
                    EsOrder.Type.SELL.name
                )
            )

            if (platform != null) {
                add(QueryBuilders.termsQuery(EsOrder::platform.name, platform.name))
            }

            if (maker != null) {
                add(QueryBuilders.termsQuery(EsOrder::maker.name, maker))
            }

            if (origin != null) {
                add(QueryBuilders.termsQuery(EsOrder::origins.name, origin))
            }

            if (status != null) {
                add(QueryBuilders.termsQuery(EsOrder::status.name, status.map { it.name }))
            }

        }

        return genericBuild(continuation, size, sort, *list.toTypedArray())
    }
}

data class EsOrderBidOrdersByItem(
    val itemId: String,
    val platform: PlatformDto?,
    val maker: List<String>?,
    val origin: String?,
    val status: List<OrderStatusDto>?,
    val continuation: DateIdContinuation?,
    val sort: OrderSortDto,
    val size: Int
) : EsOrderFilter {
    override fun asQuery(): Query {
        val list = buildList {

            val (token, tokenId) = CompositeItemIdParser.splitWithBlockchain(itemId)
            add(
                boolQuery()
                    .must(
                        QueryBuilders.termsQuery(
                            "${EsOrder::take.name}.${EsOrder.Asset::token.name}",
                            token
                        )
                    )
                    .should(
                        QueryBuilders.termsQuery(
                            "${EsOrder::take.name}.${EsOrder.Asset::tokenId.name}",
                            tokenId
                        )
                    )
                    .should(
                        boolQuery().must(
                            QueryBuilders.termsQuery(
                                "${EsOrder::take.name}.${EsOrder.Asset::isNft.name}",
                                true
                            )
                        ).mustNot(
                            QueryBuilders.existsQuery(
                                "${EsOrder::take.name}.${EsOrder.Asset::tokenId.name}"
                            )
                        )
                    )
            )

            add(
                QueryBuilders.termsQuery(
                    EsOrder::type.name,
                    EsOrder.Type.BID.name
                )
            )

            if (platform != null) {
                add(QueryBuilders.termsQuery(EsOrder::platform.name, platform.name))
            }

            if (maker != null) {
                add(QueryBuilders.termsQuery(EsOrder::maker.name, maker))
            }

            if (origin != null) {
                add(QueryBuilders.termsQuery(EsOrder::origins.name, origin))
            }

            if (status != null) {
                add(QueryBuilders.termsQuery(EsOrder::status.name, status.map { it.name }))
            }

        }

        return genericBuild(continuation, size, sort, *list.toTypedArray())
    }
}

data class EsOrdersByMakers(
    val platform: PlatformDto?,
    val maker: List<String>?,
    val origin: String?,
    val status: List<OrderStatusDto>?,
    val continuation: DateIdContinuation?,
    val size: Int,
    val sort: OrderSortDto,
    val type: EsOrder.Type
) : EsOrderFilter {
    override fun asQuery(): Query {
        val list = buildList {
            add(
                QueryBuilders.termsQuery(
                    EsOrder::type.name,
                    type.name
                )
            )

            if (platform != null) {
                add(QueryBuilders.termsQuery(EsOrder::platform.name, platform.name))
            }

            if (maker != null) {
                add(QueryBuilders.termsQuery(EsOrder::maker.name, maker))
            }

            if (origin != null) {
                add(QueryBuilders.termsQuery(EsOrder::origins.name, origin))
            }

            if (status != null) {
                add(QueryBuilders.termsQuery(EsOrder::status.name, status.map { it.name }))
            }

        }

        return genericBuild(continuation, size, sort, *list.toTypedArray())
    }
}

data class EsOrderSellOrders(
    val blockchains: List<BlockchainDto>?,
    val platform: PlatformDto?,
    val origin: String?,
    val continuation: DateIdContinuation?,
    val size: Int,
    val sort: OrderSortDto,
) : EsOrderFilter {
    override fun asQuery(): Query {
        val list = buildList {
            add(
                QueryBuilders.termsQuery(
                    EsOrder::type.name,
                    EsOrder.Type.SELL.name
                )
            )

            if (blockchains != null) {
                add(QueryBuilders.termsQuery(EsOrder::blockchain.name, blockchains.map { it.name }))
            }

            if (platform != null) {
                add(QueryBuilders.termsQuery(EsOrder::platform.name, platform.name))
            }

            if (origin != null) {
                add(QueryBuilders.termsQuery(EsOrder::origins.name, origin))
            }

        }

        return genericBuild(continuation, size, sort, *list.toTypedArray())
    }
}