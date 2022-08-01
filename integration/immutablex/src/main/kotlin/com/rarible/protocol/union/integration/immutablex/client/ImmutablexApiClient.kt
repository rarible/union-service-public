package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.integration.immutablex.client.activity.ActivityQueryBuilder
import com.rarible.protocol.union.integration.immutablex.client.activity.MintQueryBuilder
import com.rarible.protocol.union.integration.immutablex.client.activity.TradeQueryBuilder
import com.rarible.protocol.union.integration.immutablex.client.activity.TransferQueryBuilder
import com.rarible.protocol.union.integration.immutablex.client.item.AssetQueryBuilder
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
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.toEntity
import org.springframework.web.util.UriBuilder
import scalether.domain.Address
import java.time.Instant

class ImmutablexApiClient(
    private val webClient: WebClient,
) {

    val collectionsApi = CollectionsApi(webClient)

    private val creatorsRequestChunkSize = 16
    private val assetsRequestChunkSize = 16

    //-------------------- Item API ---------------------//

    suspend fun getAsset(itemId: String): ImmutablexAsset {
        val (collection, tokenId) = IdParser.split(itemId, 2)
        return webClient.get().uri("/assets/${collection}/${tokenId}?include_fees=true")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAsset::class.java)
            .awaitSingle().body!!
    }

    suspend fun getAssetsByIds(itemIds: List<String>): List<ImmutablexAsset> {
        return getChunked(assetsRequestChunkSize, itemIds) {
            try {
                getAsset(it)
            } catch (e: WebClientResponseException.NotFound) {
                null
            }
        }
    }

    suspend fun getAllAssets(
        continuation: String?,
        size: Int,
        lastUpdatedTo: Long?,
        lastUpdatedFrom: Long?,
    ) = getAssets {
        it.pageSize(size)
        it.continuation(
            lastUpdatedFrom?.let { ms -> Instant.ofEpochMilli(ms) },
            lastUpdatedTo?.let { ms -> Instant.ofEpochMilli(ms) },
            continuation
        )
    }

    suspend fun getAssetsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int,
    ) = getAssets {
        it.owner(owner)
        it.collection(collection)
        it.pageSize(size)
        it.continuation(null, null, continuation)
    }

    suspend fun getAssetsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ) = getAssets {
        it.owner(owner)
        it.pageSize(size)
        it.continuation(null, null, continuation)
    }

    private suspend fun getAssets(build: (builder: AssetQueryBuilder) -> Unit): ImmutablexAssetsPage {
        return webClient.get().uri {
            val builder = AssetQueryBuilder(it)
            build(builder)
            builder.build()
        }.accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAssetsPage::class.java)
            .awaitSingle().body!!
    }

//-------------------- Order API ---------------------//

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

//------------------- Activity API ------------------//

    suspend fun getMints(
        pageSize: Int = 50,
        continuation: String? = null,
        itemId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto? = null
    ) = activityQuery<ImmutablexMintsPage>(
        pageSize,
        continuation,
        itemId,
        from,
        to,
        user,
        sort
    ) { MintQueryBuilder(it) } ?: ImmutablexMintsPage("", false, emptyList())

    suspend fun getTransfers(
        pageSize: Int = 50,
        continuation: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto?
    ) = activityQuery<ImmutablexTransfersPage>(
        pageSize,
        continuation,
        tokenId,
        from,
        to,
        user,
        sort
    ) { TransferQueryBuilder(it) } ?: ImmutablexTransfersPage("", false, emptyList())

    suspend fun getTrades(
        pageSize: Int = 50,
        continuation: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto?
    ) = activityQuery<ImmutablexTradesPage>(
        pageSize,
        continuation,
        tokenId,
        from,
        to,
        user,
        sort
    ) { TradeQueryBuilder(it) } ?: ImmutablexTradesPage("", false, emptyList())

    private suspend inline fun <reified T> activityQuery(
        pageSize: Int,
        continuation: String?,
        itemId: String?,
        from: Instant?,
        to: Instant?,
        user: String?,
        sort: ActivitySortDto?,
        crossinline queryBuilder: (uriBuilder: UriBuilder) -> ActivityQueryBuilder
    ) = webClient.get()
        .uri {
            val builder = queryBuilder(it)
            val safeSort = sort ?: ActivitySortDto.LATEST_FIRST

            builder.itemId(itemId)
            builder.user(user)
            builder.pageSize(pageSize)
            // Sorting included
            builder.continuation(from, to, safeSort, continuation)
            builder.build()
        }
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .toEntity(T::class.java).awaitSingle().body

    suspend fun getItemCreator(itemId: String): String? {
        return getMints(pageSize = 1, itemId = itemId).result.firstOrNull()?.user
    }

    suspend fun getItemCreators(assetIds: Collection<String>): Map<String, String> {
        return getChunked(creatorsRequestChunkSize, assetIds) { itemId ->
            val creator = getMints(pageSize = 1, itemId = itemId).result.firstOrNull()?.user
            creator?.let { Pair(itemId, creator) }
        }.associateBy({ it.first }, { it.second })
    }

    private suspend fun <K, V> getChunked(chuckSize: Int, keys: Collection<K>, call: suspend (K) -> V?): List<V> {
        return coroutineScope {
            keys.chunked(chuckSize).map { chunk ->
                chunk.mapAsync { call(it) }
            }
        }.flatten().filterNotNull()
    }
}

private fun OrderStatusDto.immStatus(): String = when (this) {
    OrderStatusDto.HISTORICAL -> "expired"
    else -> this.name.lowercase()
}
