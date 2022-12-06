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
import org.elasticsearch.index.query.QueryBuilders.termsQuery
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.data.elasticsearch.core.query.Query

interface EsOrderFilter {

    fun asQuery(): Query

    fun genericBuild(
        continuation: DateIdContinuation?,
        size: Int,
        sort: EsOrderSort,
        vararg queryBuilders: QueryBuilder,
    ): Query {

        val fullQueryBuilder = queryBuilders.singleOrNull()
            ?: boolQuery().also { queryBuilders.forEach(it::must) }

        val searchQueryBuilder = NativeSearchQueryBuilder()
            .withQuery(fullQueryBuilder)
            .withMaxResults(size)

        when (sort) {
            EsOrderSort.LAST_UPDATE_DESC -> {
                searchQueryBuilder
                    .withSort(SortBuilders.fieldSort(EsOrder::lastUpdatedAt.name).order(SortOrder.DESC))
                    .withSort(SortBuilders.fieldSort(EsOrder::orderId.name).order(SortOrder.DESC))
            }
            EsOrderSort.LAST_UPDATE_ASC -> {
                searchQueryBuilder
                    .withSort(SortBuilders.fieldSort(EsOrder::lastUpdatedAt.name).order(SortOrder.ASC))
                    .withSort(SortBuilders.fieldSort(EsOrder::orderId.name).order(SortOrder.ASC))
            }
            EsOrderSort.TAKE_PRICE_DESC -> {
                searchQueryBuilder
                    .withSort(SortBuilders.fieldSort(EsOrder::takePriceUsd.name).order(SortOrder.DESC))
                    .withSort(SortBuilders.fieldSort(EsOrder::orderId.name).order(SortOrder.DESC))
            }
            EsOrderSort.MAKE_PRICE_ASC -> {
                searchQueryBuilder
                    .withSort(SortBuilders.fieldSort(EsOrder::makePriceUsd.name).order(SortOrder.ASC))
                    .withSort(SortBuilders.fieldSort(EsOrder::orderId.name).order(SortOrder.ASC))
            }
        }

        val searchQuery = searchQueryBuilder.build()

        continuation?.run {
            searchQuery.searchAfter = listOf(date.toEpochMilli(), id)
        }

        return searchQuery
    }
}

data class EsAllOrderFilter(
    private val blockchains: Collection<BlockchainDto>?,
    private val cursor: DateIdContinuation?,
    private val size: Int,
    private val sort: EsOrderSort,
    private val status: List<OrderStatusDto>?
) : EsOrderFilter {

    override fun asQuery(): Query {

        val list = buildList {
            if (blockchains != null) {
                add(termsQuery(EsOrder::blockchain.name, blockchains.map { it.name }))
            }
            if (status != null) {
                add(termsQuery(EsOrder::status.name, status.map { it.name }))
            }
        }

        return genericBuild(cursor, size, sort, *list.toTypedArray())
    }
}

enum class EsOrderSort {
    LAST_UPDATE_ASC,
    LAST_UPDATE_DESC,
    TAKE_PRICE_DESC,
    MAKE_PRICE_ASC; // getSellOrdersByItem;

    companion object {
        fun of(sort: OrderSortDto?): EsOrderSort? = sort?.let {
            when (sort) {
                OrderSortDto.LAST_UPDATE_ASC -> LAST_UPDATE_ASC
                OrderSortDto.LAST_UPDATE_DESC -> LAST_UPDATE_DESC
            }
        }
    }
}

data class EsOrderSellOrdersByItem(
    val itemId: String,
    val platform: PlatformDto?,
    val maker: String?,
    val origin: String?,
    val status: List<OrderStatusDto>?,
    val continuation: DateIdContinuation?,
    val size: Int
) : EsOrderFilter {
    override fun asQuery(): Query {
        val list = buildList {

            val (token, tokenId) = CompositeItemIdParser.splitWithBlockchain(itemId)
            add(
                boolQuery()
                    .should(
                        boolQuery().must(
                            termsQuery(
                                "${EsOrder::make.name}.${EsOrder.Asset::tokenId.name}",
                                tokenId
                            )
                        ).must(
                            termsQuery(
                                "${EsOrder::make.name}.${EsOrder.Asset::token.name}",
                                token
                            )
                        )
                    )
                    .should(
                        boolQuery().must(
                            termsQuery(
                                "${EsOrder::make.name}.${EsOrder.Asset::tokenId.name}",
                                tokenId
                            )
                        ).must(
                            termsQuery(
                                "${EsOrder::make.name}.${EsOrder.Asset::isNft.name}",
                                true
                            )
                        ).mustNot(
                            QueryBuilders.existsQuery(
                                "${EsOrder::make.name}.${EsOrder.Asset::tokenId.name}"
                            )
                        )
                    ).minimumShouldMatch(1)
            )

            add(
                termsQuery(
                    EsOrder::type.name,
                    EsOrder.Type.SELL.name
                )
            )

            if (platform != null) {
                add(termsQuery(EsOrder::platform.name, platform.name))
            }

            if (maker != null) {
                add(termsQuery(EsOrder::maker.name, maker))
            }

            if (origin != null) {
                add(termsQuery(EsOrder::origins.name, origin))
            }

            if (status != null) {
                add(termsQuery(EsOrder::status.name, status.map { it.name }))
            }

        }

        return genericBuild(continuation, size, EsOrderSort.MAKE_PRICE_ASC, *list.toTypedArray())
    }
}

data class EsOrderBidOrdersByItem(
    val itemId: String,
    val platform: PlatformDto?,
    val maker: List<String>?,
    val origin: String?,
    val status: List<OrderStatusDto>?,
    val continuation: DateIdContinuation?,
    val size: Int
) : EsOrderFilter {
    override fun asQuery(): Query {
        val list = buildList {

            val (token, tokenId) = CompositeItemIdParser.splitWithBlockchain(itemId)
            add(
                boolQuery()
                    .should(
                        boolQuery()
                            .must(
                                termsQuery(
                                    "${EsOrder::take.name}.${EsOrder.Asset::token.name}",
                                    token
                                )
                            )
                            .must(
                                termsQuery(
                                    "${EsOrder::take.name}.${EsOrder.Asset::tokenId.name}",
                                    tokenId
                                )
                            )
                    )
                    .should(
                        boolQuery().must(
                            termsQuery(
                                "${EsOrder::take.name}.${EsOrder.Asset::isNft.name}",
                                true
                            )
                        ).mustNot(
                            QueryBuilders.existsQuery(
                                "${EsOrder::take.name}.${EsOrder.Asset::tokenId.name}"
                            )
                        )
                    ).minimumShouldMatch(1)
            )

            add(
                termsQuery(
                    EsOrder::type.name,
                    EsOrder.Type.BID.name
                )
            )

            if (platform != null) {
                add(termsQuery(EsOrder::platform.name, platform.name))
            }

            if (maker != null) {
                add(termsQuery(EsOrder::maker.name, maker))
            }

            if (origin != null) {
                add(termsQuery(EsOrder::origins.name, origin))
            }

            if (status != null) {
                add(termsQuery(EsOrder::status.name, status.map { it.name }))
            }

        }

        return genericBuild(continuation, size, EsOrderSort.MAKE_PRICE_ASC, *list.toTypedArray())
    }
}

data class EsOrdersByMakers(
    val blockchains: List<BlockchainDto>?,
    val platform: PlatformDto?,
    val maker: List<String>?,
    val origin: String?,
    val status: List<OrderStatusDto>?,
    val continuation: DateIdContinuation?,
    val size: Int,
    val sort: EsOrderSort,
    val type: EsOrder.Type
) : EsOrderFilter {
    override fun asQuery(): Query {
        val list = buildList {
            add(
                termsQuery(
                    EsOrder::type.name,
                    type.name
                )
            )

            if (blockchains != null) {
                add(termsQuery(EsOrder::blockchain.name, blockchains.map { it.name }))
            }

            if (platform != null) {
                add(termsQuery(EsOrder::platform.name, platform.name))
            }

            if (maker != null) {
                add(termsQuery(EsOrder::maker.name, maker))
            }

            if (origin != null) {
                add(termsQuery(EsOrder::origins.name, origin))
            }

            if (status != null) {
                add(termsQuery(EsOrder::status.name, status.map { it.name }))
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
    val sort: EsOrderSort,
) : EsOrderFilter {
    override fun asQuery(): Query {
        val list = buildList {
            add(
                termsQuery(
                    EsOrder::type.name,
                    EsOrder.Type.SELL.name
                )
            )

            if (blockchains != null) {
                add(termsQuery(EsOrder::blockchain.name, blockchains.map { it.name }))
            }

            if (platform != null) {
                add(termsQuery(EsOrder::platform.name, platform.name))
            }

            if (origin != null) {
                add(termsQuery(EsOrder::origins.name, origin))
            }

        }

        return genericBuild(continuation, size, sort, *list.toTypedArray())
    }
}