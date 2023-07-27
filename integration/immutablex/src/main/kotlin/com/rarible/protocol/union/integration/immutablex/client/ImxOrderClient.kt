package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity

class ImxOrderClient(
    webClient: WebClient,
    private val byIdsChunkSize: Int
) : AbstractImxClient(
    webClient
) {

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

    private val orderComparator = compareBy<ImmutablexOrder>({ it.updatedAt }, { it.orderId.toString() })

    suspend fun getById(id: String): ImmutablexOrder {
        return getByUri(ImxOrderQueryBuilder.getByIdPath(id))
    }

    suspend fun getByIds(orderIds: Collection<String>): List<ImmutablexOrder> {
        return getChunked(byIdsChunkSize, orderIds) {
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
            getOrders {
                it.pageSize(size)
                // TODO bids disabled for IMX
                it.sellTokenType("ERC721")
                it.continuationByUpdatedAt(safeSort, continuation)
                it.status(status)
            }
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

    // Workaround to get some currencies
    suspend fun getSellOrdersByItem(
        token: String,
        tokenId: String,
        status: OrderStatusDto?,
        continuation: String?,
        size: Int,
    ): ImmutablexOrdersPage {
        return getOrders {
            it.pageSize(size)
            it.sellTokenType("ERC721")
            it.sellToken(token)
            it.sellTokenId(tokenId)
            it.status(status)
            it.continuationByUpdatedAt(OrderSortDto.LAST_UPDATE_DESC, continuation)
        }
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
            getOrders {
                it.pageSize(size)
                it.sellTokenType("ERC721")
                it.continuationBySellPrice(currencyId, continuation)
                it.sellToken(token)
                it.sellTokenId(tokenId)
                it.maker(maker)
                it.status(status)
            }
        }

        return mergeSellPages(pages, size)
    }

    suspend fun getSellOrdersByMaker(
        makers: List<String>,
        statuses: List<OrderStatusDto>?,
        continuation: String?,
        size: Int,
    ): List<ImmutablexOrder> {
        val filteredStatuses = statuses?.filter { isSupportedSellStatus(it) }
        val pages = getOrdersByStatuses(filteredStatuses) { status ->
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

        return getOrders {
            it.pageSize(size)
            it.sellTokenType("ERC721")
            it.continuationByUpdatedAt(safeSort, continuation)
            it.sellToken(collection)
            it.sellTokenId(tokenId)
            it.maker(maker)
            it.status(status)
        }
    }

    suspend fun getBuyOrdersByMaker(
        makers: List<String>,
        statuses: List<OrderStatusDto>?,
        continuation: String?,
        size: Int,
    ): List<ImmutablexOrder> {
        val filteredStatuses = statuses?.filter { isSupportedBuyStatus(it) }
        val pages = getOrdersByStatuses(filteredStatuses) { status ->
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

    // Workaround to get some currencies
    suspend fun getBuyOrdersByItem(
        token: String,
        tokenId: String,
        continuation: String?,
        size: Int,
    ): ImmutablexOrdersPage {
        return getOrders {
            it.buyTokenType("ERC721")
            it.buyToken(token)
            it.buyTokenId(tokenId)
            it.pageSize(size)
            it.continuationByUpdatedAt(OrderSortDto.LAST_UPDATE_DESC, continuation)
        }
    }

    suspend fun getBuyOrdersByItem(
        token: String,
        tokenId: String,
        makers: List<String>? = null,
        statuses: List<OrderStatusDto>? = null,
        currencyId: String,
        continuation: String?,
        size: Int,
    ): List<ImmutablexOrder> {
        val filteredStatuses = statuses?.filter { isSupportedBuyStatus(it) }
        val pages = getOrdersByStatuses(filteredStatuses) { status ->
            val makerPages = getOrdersByMaker(makers) { maker ->
                getOrders {
                    it.buyTokenType("ERC721")
                    it.buyToken(token)
                    it.buyTokenId(tokenId)
                    it.maker(maker)
                    it.status(status)
                    it.pageSize(size)
                    it.continuationByBuyPrice(currencyId, continuation)
                }
            }
            ImmutablexOrdersPage(mergePages(makerPages, null, size))
        }

        return mergeBuyPages(pages, size)
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

        return getOrders {
            it.buyTokenType("ERC721")
            it.buyToken(collection)
            it.buyTokenId(tokenId)
            it.maker(maker)
            it.status(status)
            it.pageSize(size)
            it.continuationByUpdatedAt(safeSort, continuation)
        }
    }

    private suspend fun getOrdersByStatuses(
        statuses: List<OrderStatusDto>?,
        call: suspend (status: OrderStatusDto?) -> ImmutablexOrdersPage
    ): List<ImmutablexOrdersPage> {
        return statuses?.mapAsync { call(it) } ?: listOf(call(null))
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

    private suspend fun getOrders(build: (builder: ImxOrderQueryBuilder) -> Unit): ImmutablexOrdersPage {
        return webClient.get().uri {
            val builder = ImxOrderQueryBuilder(it)
            build(builder)
            builder.build()
        }.retrieve()
            .toEntity<ImmutablexOrdersPage>()
            .awaitSingle().body!!
    }

    private fun mergePages(
        pages: Collection<ImmutablexOrdersPage>,
        sort: OrderSortDto?,
        size: Int
    ): List<ImmutablexOrder> {
        val safeSort = sort ?: OrderSortDto.LAST_UPDATE_DESC
        val orders = pages.map { it.result }.flatten()

        val sorted = when (safeSort) {
            OrderSortDto.LAST_UPDATE_ASC -> orders.sortedWith(orderComparator)
            OrderSortDto.LAST_UPDATE_DESC -> orders.sortedWith(orderComparator.reversed())
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
