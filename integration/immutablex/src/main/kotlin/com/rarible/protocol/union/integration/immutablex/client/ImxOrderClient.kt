package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity

class ImxOrderClient(
    webClient: WebClient,
) : AbstractImxClient(
    webClient
) {

    // TODO IMMUTABLEX move out to configuration
    private val orderRequestChunkSize = 16

    private val supportedBuyOrderStatuses = setOf(
        OrderStatusDto.FILLED,
        OrderStatusDto.CANCELLED
    )
    private val supportedSellOrderStatuses = setOf(
        OrderStatusDto.FILLED,
        OrderStatusDto.CANCELLED,
        OrderStatusDto.ACTIVE,
        OrderStatusDto.INACTIVE
    )

    suspend fun getById(id: String): ImmutablexOrder {
        return getByUri(ImxOrderQueryBuilder.getByIdPath(id))
    }

    suspend fun getByIds(orderIds: Collection<String>): List<ImmutablexOrder> {
        return getChunked(orderRequestChunkSize, orderIds) {
            ignore404 { getById(it) }
        }
    }

    suspend fun getAllOrders(
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        statuses: List<OrderStatusDto>?,
    ): List<ImmutablexOrder> {
        val pages = getOrdersByStatuses(statuses) { status ->
            val safeSort = sort ?: OrderSortDto.LAST_UPDATE_DESC
            webClient.get().uri {
                val builder = ImxOrderQueryBuilder(it)
                builder.pageSize(size)
                builder.continuation(safeSort, continuation)
                builder.status(status)
                builder.build()
            }.retrieve()
                .toEntity<ImmutablexOrdersPage>()
                .awaitSingle().body!!
        }
        return mergePages(pages, sort, size)
    }

    suspend fun getSellOrders(
        continuation: String?,
        size: Int
    ): List<ImmutablexOrder> {
        val page = sellOrders(
            continuation = continuation,
            size = size
        )
        return mergePages(listOf(page), null, size)
    }

    suspend fun getSellOrdersByCollection(
        collection: String,
        continuation: String?,
        size: Int
    ): List<ImmutablexOrder> {
        val page = sellOrders(
            continuation = continuation,
            size = size,
            collection = collection
        )
        return mergePages(listOf(page), null, size)
    }

    suspend fun getSellOrdersByItem(
        token: String,
        tokenId: String,
        maker: String?,
        statuses: List<OrderStatusDto>?,
        currencyId: String,
        continuation: String?,
        size: Int,
    ): List<ImmutablexOrder> {
        val filteredStatuses = statuses?.filter { isSupportedSellStatus(it) }
        val pages = getOrdersByStatuses(filteredStatuses) { status ->
            webClient.get().uri {
                val builder = ImxOrderQueryBuilder(it)
                builder.pageSize(size)
                builder.sellTokenType("ERC721")
                builder.sellPriceContinuation(currencyId, continuation)
                builder.sellToken(token)
                builder.sellTokenId(tokenId)
                builder.maker(maker)
                builder.status(status)
                builder.build()
            }.retrieve()
                .toEntity<ImmutablexOrdersPage>()
                .awaitSingle().body!!
        }

        return mergeSellPages(pages, size)
    }

    suspend fun getSellOrdersByMaker(
        makers: List<String>,
        statuses: List<OrderStatusDto>?,
        continuation: String?,
        size: Int,
    ): List<ImmutablexOrder> {
        val pages = getOrdersByStatuses(statuses) { status ->
            val makerPages = getOrdersByMaker(makers) { maker ->
                sellOrders(
                    continuation = continuation,
                    size = size,
                    maker = maker,
                    status = status
                )
            }
            ImmutablexOrdersPage("", false, mergePages(makerPages, null, size))
        }
        return mergePages(pages, null, size)
    }

    private suspend fun sellOrders(
        continuation: String?,
        size: Int,
        sort: OrderSortDto? = null,
        status: OrderStatusDto? = null,
        maker: String? = null,
        collection: String? = null,
        tokenId: String? = null
    ): ImmutablexOrdersPage {
        if (!isSupportedSellStatus(status)) return ImmutablexOrdersPage.empty()

        val safeSort = sort ?: OrderSortDto.LAST_UPDATE_DESC
        return webClient.get().uri {
            val builder = ImxOrderQueryBuilder(it)
            builder.pageSize(size)
            builder.sellTokenType("ERC721")
            builder.continuation(safeSort, continuation)
            builder.sellToken(collection)
            builder.sellTokenId(tokenId)
            builder.maker(maker)
            builder.status(status)
            builder.build()
        }.retrieve()
            .toEntity<ImmutablexOrdersPage>()
            .awaitSingle().body!!
    }

    suspend fun getBuyOrdersByMaker(
        makers: List<String>,
        statuses: List<OrderStatusDto>?,
        continuation: String?,
        size: Int,
    ): List<ImmutablexOrder> {

        val pages = getOrdersByStatuses(statuses) { status ->
            val makerPages = getOrdersByMaker(makers) { maker ->
                buyOrders(
                    continuation = continuation,
                    size = size,
                    maker = maker,
                    status = status
                )
            }
            ImmutablexOrdersPage(mergePages(makerPages, null, size))
        }

        return mergePages(pages, null, size)
    }

    suspend fun getBuyOrdersByItem(
        token: String,
        tokenId: String,
        makers: List<String>?,
        statuses: List<OrderStatusDto>?,
        currencyId: String,
        continuation: String?,
        size: Int,
    ): List<ImmutablexOrder> {
        val filteredStatuses = statuses?.filter { isSupportedBuyStatus(it) }
        val pages = getOrdersByStatuses(filteredStatuses) { status ->
            val makerPages = getOrdersByMaker(makers) { maker ->
                webClient.get().uri {
                    val builder = ImxOrderQueryBuilder(it)
                    builder.buyTokenType("ERC721")
                    builder.buyToken(token)
                    builder.buyTokenId(tokenId)
                    builder.maker(maker)
                    builder.status(status)
                    builder.pageSize(size)
                    builder.buyPriceContinuation(currencyId, continuation)
                    builder.build()
                }.retrieve()
                    .toEntity<ImmutablexOrdersPage>()
                    .awaitSingle().body!!
            }
            ImmutablexOrdersPage(mergePages(makerPages, null, size))
        }

        return mergeSellPages(pages, size)
    }

    private suspend fun buyOrders(
        continuation: String?,
        size: Int,
        sort: OrderSortDto? = null,
        status: OrderStatusDto? = null,
        maker: String? = null,
        collection: String? = null,
        tokenId: String? = null
    ): ImmutablexOrdersPage {
        if (!isSupportedBuyStatus(status)) return ImmutablexOrdersPage.empty()

        val safeSort = sort ?: OrderSortDto.LAST_UPDATE_DESC
        return webClient.get().uri {
            val builder = ImxOrderQueryBuilder(it)
            builder.buyTokenType("ERC721")
            builder.buyToken(collection)
            builder.buyTokenId(tokenId)
            builder.maker(maker)
            builder.status(status)
            builder.pageSize(size)
            builder.continuation(safeSort, continuation)
            builder.build()
        }.retrieve()
            .toEntity<ImmutablexOrdersPage>()
            .awaitSingle().body!!
    }

    private suspend fun getOrdersByStatuses(
        statuses: List<OrderStatusDto>?,
        call: suspend (status: OrderStatusDto?) -> ImmutablexOrdersPage
    ): List<ImmutablexOrdersPage> {
        return if (statuses.isNullOrEmpty()) {
            listOf(call(null))
        } else {
            statuses.mapAsync { call(it) }
        }
    }

    private suspend fun getOrdersByMaker(
        makers: List<String>?,
        call: suspend (maker: String?) -> ImmutablexOrdersPage
    ): List<ImmutablexOrdersPage> {
        return if (makers.isNullOrEmpty()) {
            listOf(call(null))
        } else {
            makers.mapAsync { call(it) }
        }
    }

    private fun mergePages(
        pages: Collection<ImmutablexOrdersPage>,
        sort: OrderSortDto?,
        size: Int
    ): List<ImmutablexOrder> {
        val safeSort = sort ?: OrderSortDto.LAST_UPDATE_DESC
        val orders = pages.map { it.result }.flatten()

        val sorted = when (safeSort) {
            // TODO Ideally we should consider here ID too
            OrderSortDto.LAST_UPDATE_ASC -> orders.sortedBy { it.updatedAt }
            OrderSortDto.LAST_UPDATE_DESC -> orders.sortedByDescending { it.updatedAt }
        }

        return sorted.take(size)
    }

    private fun mergeSellPages(pages: Collection<ImmutablexOrdersPage>, size: Int): List<ImmutablexOrder> {
        return pages.map { it.result }.flatten()
            .sortedBy { it.buy.data.quantity }
            .take(size)
    }

    private fun mergeBuyPages(pages: Collection<ImmutablexOrdersPage>, size: Int): List<ImmutablexOrder> {
        return pages.map { it.result }.flatten()
            .sortedByDescending { it.sell.data.quantity }
            .take(size)
    }

    private fun isSupportedSellStatus(status: OrderStatusDto?): Boolean {
        return status == null || supportedSellOrderStatuses.contains(status)
    }

    private fun isSupportedBuyStatus(status: OrderStatusDto?): Boolean {
        return status == null || supportedBuyOrderStatuses.contains(status)
    }
}

