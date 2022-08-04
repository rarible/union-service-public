package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexOrdersPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import scalether.domain.Address
import java.time.Instant

class ImmutablexOrderClient(
    webClient: WebClient,
) : AbstractImmutablexClient(
    webClient
) {

    suspend fun getById(id: Long): ImmutablexOrder {
        val uri = "/orders/$id?include_fees=true"
        return getByUri(uri)
    }

    suspend fun getAllOrders(
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?,
    ): List<ImmutablexOrder> {
        val direction = sort ?: OrderSortDto.LAST_UPDATE_DESC
        if (!status.isNullOrEmpty()) {
            val pages = status.map {
                coroutineScope {
                    async(Dispatchers.IO) {
                        ordersByStatus(continuation, size, it, direction)
                    }
                }
            }.awaitAll()
            val orders = pages.flatMap { it.result }
            return when (direction) {
                OrderSortDto.LAST_UPDATE_ASC -> orders.sortedBy { it.updatedAt }
                OrderSortDto.LAST_UPDATE_DESC -> orders.sortedByDescending { it.updatedAt }
            }
        }

        return ordersByStatus(continuation, size, direction = direction).result
    }

    suspend fun getSellOrders(continuation: String?, size: Int): List<ImmutablexOrder> {
        val continuationFrom = DateIdContinuation.parse(continuation)?.date
        return webClient.get().uri {
            it.path("/orders")
                .queryParam("page_size", size)
                .queryParam("include_fees", true)
                .queryParam("sell_token_type", "ERC721")
                .queryParam("order_by", "updated_at")
                .queryParam("direction", "desc")
                .queryParamNotNull("updated_min_timestamp", continuationFrom)
                .build()
        }.retrieve()
            .toEntity<ImmutablexOrdersPage>()
            .awaitSingle().body!!.result
    }

    suspend fun getSellOrdersByCollection(collection: String, continuation: String?, size: Int): List<ImmutablexOrder> {
        val continuationFrom = DateIdContinuation.parse(continuation)?.date
        return webClient.get().uri {
            it.path("/orders")
                .queryParam("page_size", size)
                .queryParam("include_fees", true)
                .queryParam("sell_token_type", "ERC721")
                .queryParam("order_by", "updated_at")
                .queryParam("direction", "desc")
                .queryParam("sell_token_address", collection)
                .queryParamNotNull("updated_min_timestamp", continuationFrom)
                .build()
        }.retrieve()
            .toEntity<ImmutablexOrdersPage>()
            .awaitSingle().body!!.result
    }

    suspend fun getSellOrdersByItem(
        itemId: String,
        maker: String?,
        status: List<OrderStatusDto>?,
        currencyId: String,
        continuation: String?,
        size: Int,
    ): List<ImmutablexOrder> {
        val (tokenAddress, tokenId) = itemId.split(":")
        val params = mutableMapOf<String, Any>()
        params["sell_token_address"] = tokenAddress
        params["sell_token_id"] = String(tokenId.toBigInteger().toByteArray())
        if (currencyId != "${Address.ZERO()}") {
            params["buy_token_address"] = currencyId
        }

        if (maker != null) {
            params["user"] = maker
        }

        if (status != null) {
            val pages = status.map {
                coroutineScope {
                    async(Dispatchers.IO) {
                        ordersByStatus(continuation, size, it, OrderSortDto.LAST_UPDATE_DESC, params)
                    }
                }
            }.awaitAll()
            return pages.flatMap { it.result }.sortedByDescending { it.updatedAt }
        }
        return ordersByStatus(continuation, size, null, OrderSortDto.LAST_UPDATE_DESC, params).result
    }

    suspend fun getSellOrdersByMaker(
        makers: List<String>,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int,
    ): List<ImmutablexOrder> {
        val pages = makers.flatMap { maker ->
            val params = mapOf("user" to maker)
            if (status != null) {
                return@flatMap status.map {
                    coroutineScope {
                        async(Dispatchers.IO) {
                            ordersByStatus(continuation, size, it, OrderSortDto.LAST_UPDATE_DESC, params)
                        }
                    }
                }
            } else {
                return@flatMap listOf(coroutineScope {
                    async(Dispatchers.IO) {
                        ordersByStatus(continuation, size, null, OrderSortDto.LAST_UPDATE_DESC, params)
                    }
                })
            }
        }.awaitAll()
        return pages.flatMap { it.result }.sortedByDescending { it.updatedAt }
    }

    private suspend fun ordersByStatus(
        continuation: String?,
        size: Int,
        status: OrderStatusDto? = null,
        direction: OrderSortDto,
        additionalQueryParams: Map<String, Any> = emptyMap(),
    ): ImmutablexOrdersPage {
        return webClient.get().uri { uriBuilder ->
            uriBuilder.path("/orders")
                .queryParam("page_size", size)
                .queryParam("order_by", "updated_at")
                .queryParam(
                    "direction", when (direction) {
                    OrderSortDto.LAST_UPDATE_DESC -> "DESC"
                    OrderSortDto.LAST_UPDATE_ASC -> "ASC"
                }
                )
                .queryParam("include_fees", true)
            if (!continuation.isNullOrEmpty()) {
                val (dateStr, _) = continuation.split("_")
                uriBuilder.queryParam("updated_min_timestamp", "${Instant.ofEpochMilli(dateStr.toLong())}")
            }
            if (status != null) {
                uriBuilder.queryParam("status", status.immStatus())
            }

            additionalQueryParams.forEach { (k, v) ->
                uriBuilder.queryParam(k, v)
            }
            uriBuilder.build()
        }.retrieve()
            .toEntity<ImmutablexOrdersPage>()
            .awaitSingle().body!!
    }
}

private fun OrderStatusDto.immStatus(): String = when (this) {
    OrderStatusDto.HISTORICAL -> "expired"
    else -> this.name.lowercase()
}
