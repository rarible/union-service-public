package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.integration.immutablex.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import java.time.Instant

class ImmutablexApiClient(
    private val webClient: WebClient,
) {

    val collectionsApi = CollectionsApi(webClient)

    suspend fun getAsset(itemId: String): ImmutablexAsset {
        val (collection, tokenId) = itemId.split(":")
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
        val query = StringBuilder("?order_by=updated_at&direction=desc&include_fees=true")
        query.append("&page_size=$size")
        if (lastUpdatedFrom != null) {
            query.append("&updated_min_timestamp=$lastUpdatedFrom")
        }
        if (lastUpdatedTo != null) {
            query.append("&updated_max_timestamp=$lastUpdatedTo")
        }
        if (!continuation.isNullOrEmpty()) {
            query.append("&cursor=$continuation")
        }
        return webClient.get().uri("$/assets$query").accept(MediaType.APPLICATION_JSON)
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
        val query = StringBuilder("?order_by=updated_at&direction=desc&include_fees=true")
        query.append("&collection=$collection")
        query.append("&page_size=$size")
        if (!owner.isNullOrEmpty()) {
            query.append("&user=$owner")
        }
        if (!continuation.isNullOrEmpty()) {
            query.append("&cursor=$continuation")
        }
        return webClient.get().uri("/assets$query").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAssetsPage::class.java)
            .awaitSingle().body!!
    }

    suspend fun getAssetsByOwner(owner: String, continuation: String?, size: Int): ImmutablexAssetsPage {
        val query = StringBuilder("?order_by=updated_at&direction=desc&include_fees=true")
        query.append("&user=$owner")
        query.append("&page_size=$size")
        if (!continuation.isNullOrEmpty()) {
            query.append("&cursor=$continuation")
        }
        return webClient.get().uri("/assets$query").accept(MediaType.APPLICATION_JSON)
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
                mints.result.map { it.token.data.tokenId!! }
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
            return when(direction) {
                OrderSortDto.LAST_UPDATE_ASC -> orders.sortedBy { it.updatedAt }
                OrderSortDto.LAST_UPDATE_DESC -> orders.sortedByDescending { it.updatedAt }
            }
        }

        return ordersByStatus(continuation, size, direction = direction).result
    }

    suspend fun getSellOrders(continuation: String?, size: Int): List<ImmutablexOrder> {
        return webClient.get().uri {
            it.path("/orders")
                .queryParam("page_size", size)
                .queryParam("include_fees", true)
                .queryParam("sell_token_type", "ERC721")
                .queryParam("order_by", "updated_at")
                .queryParam("direction", "desc")
            if (!continuation.isNullOrEmpty()) {
                val (dateStr, tokenIdStr) = continuation.split("_")
                it.queryParam("updated_min_timestamp", dateStr)
            }
            it.build()
        }.retrieve()
            .toEntity<ImmutablexOrdersPage>()
            .awaitSingle().body!!.result
    }

    suspend fun getSellOrdersByCollection(collection: String, continuation: String?, size: Int): List<ImmutablexOrder> {
        return webClient.get().uri {
            it.path("/orders")
                .queryParam("page_size", size)
                .queryParam("include_fees", true)
                .queryParam("sell_token_type", "ERC721")
                .queryParam("order_by", "updated_at")
                .queryParam("direction", "desc")
                .queryParam("sell_token_address", collection)
            if (!continuation.isNullOrEmpty()) {
                val (dateStr, tokenIdStr) = continuation.split("_")
                it.queryParam("updated_min_timestamp", dateStr)
            }
            it.build()
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
        params["sell_token_id"] = tokenId
        params["buy_token_address"] = currencyId

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
        maker: String,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int,
    ): List<ImmutablexOrder> {
        val params = mapOf("user" to maker)
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

    suspend fun getMints(
        pageSize: Int = 50,
        continuation: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
    ) = activityQuery<ImmutablexMintsPage>(
        "/mints",
        pageSize,
        continuation,
        tokenId,
        from,
        to,
        user
    ) ?: ImmutablexMintsPage("", false, emptyList())

    suspend fun getTransfers(
        pageSize: Int = 50,
        continuation: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
    ) = activityQuery<ImmutablexTransfersPage>(
        "/transfers",
        pageSize,
        continuation,
        tokenId,
        from,
        to,
        user
    ) ?: ImmutablexTransfersPage("", false, emptyList())

    suspend fun getTrades(
        pageSize: Int = 50,
        continuation: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
    ) = activityQuery<ImmutablexTradesPage>(
        "/trades",
        pageSize,
        continuation,
        tokenId,
        from,
        to,
        user
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
                .queryParam("direction", direction.name.lowercase())
                .queryParam("include_fees", true)
            if (!continuation.isNullOrEmpty()) {
                val (dateStr, idStr) = continuation.split("_")
                uriBuilder.queryParam("updated_min_timestamp", dateStr)
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
        tokenId: String?,
        from: Instant?,
        to: Instant?,
        user: String?,
    ) = webClient.get()
        .uri {
            it.path(path)
            it.queryParam("page_size", pageSize)

            val c = listOfNotNull(from, getDateFromContinuation(continuation)).maxOrNull()
            if (c != null) {
                it.queryParam("min_timestamp", c)
            }
            if (to != null) {
                it.queryParam("max_timestamp", to)
            }
            if (tokenId != null) {
                it.queryParam("token_id", tokenId)
            }
            if (user != null) {
                it.queryParam("user", user)
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
