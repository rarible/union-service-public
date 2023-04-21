package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import java.math.BigInteger

interface DipdupOrderService {

    suspend fun getOrderById(id: String): UnionOrder {
        TODO("Not implemented")
    }

    suspend fun getOrderByIds(id: List<String>): List<UnionOrder> {
        TODO("Not implemented")
    }

    suspend fun getOrdersAll(
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?,
        isBid: Boolean? = null,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        TODO("Not implemented")
    }

    suspend fun getOrdersAllSync(
        continuation: String?,
        limit: Int,
        sort: SyncSortDto?
    ): Slice<UnionOrder> {
        TODO("Not implemented")
    }

    suspend fun getSellOrders(
        origin: String?,
        platforms: List<TezosPlatform>,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        TODO("Not implemented")
    }

    suspend fun getSellOrdersByCollection(
        contract: String,
        origin: String?,
        platforms: List<TezosPlatform>,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        TODO("Not implemented")
    }

    suspend fun getSellOrdersByItem(
        contract: String,
        tokenId: BigInteger,
        maker: String?,
        platforms: List<TezosPlatform>,
        currencyId: String,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        TODO("Not implemented")
    }

    suspend fun getSellOrdersByMaker(
        maker: List<String>,
        platforms: List<TezosPlatform>,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        TODO("Not implemented")
    }

    suspend fun getSellOrderCurrenciesByItem(contract: String, tokenId: BigInteger): List<UnionAssetType> {
        TODO("Not implemented")
    }

    suspend fun getSellOrderCurrenciesByCollection(contract: String): List<UnionAssetType> {
        TODO("Not implemented")
    }

    suspend fun getBidOrdersByItem(
        contract: String,
        tokenId: BigInteger,
        maker: String?,
        platforms: List<TezosPlatform>,
        currencyId: String,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        TODO("Not implemented")
    }

    suspend fun getBidOrdersByMaker(
        maker: List<String>,
        platforms: List<TezosPlatform>,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        TODO("Not implemented")
    }

    suspend fun getBidOrderCurrenciesByItem(contract: String, tokenId: BigInteger): List<UnionAssetType> {
        TODO("Not implemented")
    }

    suspend fun getBidOrderCurrenciesByCollection(contract: String): List<UnionAssetType> {
        TODO("Not implemented")
    }
}


