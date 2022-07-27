package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAssetsPage
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexMintsPage
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexOrdersPage
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTradesPage
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTransfersPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

class ImmutablexApiClient(
    private val webClient: WebClient,
) {

    val collectionsApi = CollectionsApi(webClient)

    suspend fun getAsset(itemId: String): ImmutablexAsset {
        val (collection, tokenId) = IdParser.split(itemId, 2)
        return webClient.get().uri("/assets/${collection}/${tokenId}?include_fees=true")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAsset::class.java)
            .awaitSingle().body!!
    }

    suspend fun getAllAssets(
        continuation: String?,
        size: Int,
        lastUpdatedTo: Long?,
        lastUpdatedFrom: Long?,
    ): ImmutablexAssetsPage {
        val from = lastUpdatedFrom?.let { Instant.ofEpochMilli(it) }
        val continuationFrom = DateIdContinuation.parse(continuation)?.date
        val queryFrom = listOfNotNull(from, continuationFrom).maxOrNull()

        val to = lastUpdatedTo?.let { Instant.ofEpochMilli(it) }

        return webClient.get().uri {
            it.path("/assets")
                .queryParam("order_by", "updated_at")
                .queryParam("direction", "desc")
                .queryParam("include_fees", true)
                .queryParam("page_size", size)
                .queryParamNotNull("updated_max_timestamp", queryFrom)
                .queryParamNotNull("updated_min_timestamp", to)
                .build()
        }.accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAssetsPage::class.java)
            .awaitSingle().body!!

    }

    suspend fun getAssetsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int,
    ): ImmutablexAssetsPage {
        return webClient.get().uri {
            it.path("/assets")
                .queryParam("order_by", "updated_at")
                .queryParam("direction", "desc")
                .queryParam("include_fees", true)
                .queryParam("page_size", size)
                .queryParam("collection", collection)
                .queryParamNotNull("user", owner)
                .queryParamNotNull("cursor", continuation)
                .build()
        }.accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAssetsPage::class.java)
            .awaitSingle().body!!
    }

    suspend fun getAssetsByOwner(owner: String, continuation: String?, size: Int): ImmutablexAssetsPage {
        val continuationFrom = DateIdContinuation.parse(continuation)?.date
        return webClient.get().uri {
            it.path("/assets")
                .queryParam("order_by", "updated_at")
                .queryParam("direction", "desc")
                .queryParam("include_fees", true)
                .queryParam("page_size", size)
                .queryParamNotNull("user", owner)
                .queryParamNotNull("updated_max_timestamp", continuationFrom)
                .build()
        }.accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAssetsPage::class.java)
            .awaitSingle().body!!
    }

    suspend fun getAssetsByIds(itemIds: List<String>): List<ImmutablexAsset> {
        val list = itemIds.map {
            coroutineScope {
                async(Dispatchers.IO) { getAsset(it) }
            }
        }
        return list.awaitAll()
    }

    suspend fun getAssetsByCreator(creator: String, continuation: String?, size: Int): ImmutablexAssetsPage {
        val query = StringBuilder("?page_size=$size&user=$creator")
        if (!continuation.isNullOrEmpty()) {
            query.append("&cursor=$continuation")
        }

        val mints = webClient.get().uri("/mints$query").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexMintsPage::class.java)
            .awaitSingle().body!!

        return ImmutablexAssetsPage(
            cursor = mints.cursor,
            remaining = mints.remaining,
            result = getAssetsByIds(
                mints.result.map { "${it.token.data.tokenAddress}:${BigInteger(it.token.data.tokenId.toByteArray())}" }
            )
        )
    }

    suspend fun getOrderById(id: Long): ImmutablexOrder {
        return webClient.get().uri("/orders/$id?include_fees=true")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexOrder::class.java)
            .awaitSingle().body!!
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

    suspend fun getMints(
        pageSize: Int = 50,
        continuation: String? = null,
        itemId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto? = null
    ) = activityQuery<ImmutablexMintsPage>(
        "/mints",
        pageSize,
        continuation,
        itemId,
        from,
        to,
        user,
        sort
    ) ?: ImmutablexMintsPage("", false, emptyList())

    suspend fun getTransfers(
        pageSize: Int = 50,
        continuation: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto?
    ) = activityQuery<ImmutablexTransfersPage>(
        "/transfers",
        pageSize,
        continuation,
        tokenId,
        from,
        to,
        user,
        sort
    ) ?: ImmutablexTransfersPage("", false, emptyList())

    suspend fun getTrades(
        pageSize: Int = 50,
        continuation: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto?
    ) = activityQuery<ImmutablexTradesPage>(
        "/trades",
        pageSize,
        continuation,
        tokenId,
        from,
        to,
        user,
        sort,
        "party_b_token_address",
        "party_b_token_id"
    ) ?: ImmutablexTradesPage("", false, emptyList())

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

    private fun getDateFromContinuation(continuation: String?): Instant? {
        if (!continuation.isNullOrEmpty()) {
            val (d, _) = continuation.split("_")
            val millis = d.toLongOrNull()
            if (millis != null) {
                return Instant.ofEpochMilli(millis)
            }
        }
        return null
    }

    private suspend inline fun <reified T> activityQuery(
        path: String,
        pageSize: Int,
        continuation: String?,
        itemId: String?,
        from: Instant?,
        to: Instant?,
        user: String?,
        sort: ActivitySortDto?,
        tokenAddressParamName: String = "token_address",
        tokenIdParamName: String = "token_id"
    ) = webClient.get()
        .uri {
            val continuationFrom = DateIdContinuation.parse(continuation)?.date
            val queryFrom = listOfNotNull(from, continuationFrom).maxOrNull()

            it.path(path)
            it.queryParam("page_size", pageSize)

            it.queryParamNotNull("min_timestamp", queryFrom)
            it.queryParamNotNull("max_timestamp", to)

            it.queryParamNotNull("user", user)

            if (itemId != null) {
                val (address, id) = IdParser.split(itemId, 2)
                it.queryParam(tokenAddressParamName, address)
                it.queryParam(tokenIdParamName, id)
            }

            if (sort != null) {
                it.queryParam("order_by", "updated_at")
                it.queryParam(
                    "direction", when (sort) {
                    ActivitySortDto.LATEST_FIRST -> "desc"
                    ActivitySortDto.EARLIEST_FIRST -> "asc"
                }
                )
            }
            it.build()
        }
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .toEntity(T::class.java).awaitSingle().body
}

private fun OrderStatusDto.immStatus(): String = when (this) {
    OrderStatusDto.HISTORICAL -> "expired"
    else -> this.name.lowercase()
}
