package com.rarible.protocol.union.enrichment.service.query.order

import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Slice

interface OrderQueryService {

    suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        statuses: List<OrderStatusDto>?
    ): Slice<UnionOrder>

    suspend fun getAllSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?
    ): Slice<UnionOrder>

    suspend fun getSellOrdersByItem(
        itemId: ItemIdDto,
        platform: PlatformDto?,
        maker: UnionAddress?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): Slice<UnionOrder>

    suspend fun getOrderBidsByItem(
        itemId: ItemIdDto,
        platform: PlatformDto?,
        makers: List<UnionAddress>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencies: List<CurrencyIdDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): Slice<UnionOrder>

    suspend fun getOrderBidsByMaker(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        makers: List<UnionAddress>,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencies: List<CurrencyIdDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): Slice<UnionOrder>

    suspend fun getSellOrders(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): Slice<UnionOrder>

    suspend fun getSellOrdersByMaker(
        makers: List<UnionAddress>,
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): Slice<UnionOrder>
}
