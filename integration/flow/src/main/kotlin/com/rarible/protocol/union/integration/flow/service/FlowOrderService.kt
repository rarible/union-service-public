package com.rarible.protocol.union.integration.flow.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.FlowOrderIdsDto
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.converter.UnionConverter
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.flow.converter.FlowConverter
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import kotlinx.coroutines.reactive.awaitFirst
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@CaptureSpan(type = "network", subtype = "flow")
open class FlowOrderService(
    private val orderControllerApi: FlowOrderControllerApi,
    private val flowOrderConverter: FlowOrderConverter
) : AbstractBlockchainService(BlockchainDto.FLOW), OrderService {

    override suspend fun getOrdersAll(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val result = orderControllerApi.getOrdersAll(
            origin,
            continuation,
            size
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getOrderById(id: String): OrderDto {
        val order = orderControllerApi.getOrderByOrderId(id).awaitFirst()
        return flowOrderConverter.convert(order, blockchain)
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<OrderDto> {
        val ids = orderIds.map { it.toLong() }
        val orders = orderControllerApi.getOrdersByIds(FlowOrderIdsDto(ids)).collectList().awaitFirst()
        return orders.map { flowOrderConverter.convert(it, blockchain) }
    }

    override suspend fun getBidCurrencies(contract: String, tokenId: String): List<AssetTypeDto> {
        UnionConverter.convertToLong(tokenId) // Validation
        val assets = orderControllerApi.getBidCurrencies("$contract:$tokenId")
            .collectList().awaitFirst()
        return assets.map { FlowConverter.convert(it, blockchain).type }
    }

    override suspend fun getOrderBidsByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        currencyAddress: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val result = orderControllerApi.getBidsByItem(
            contract,
            UnionConverter.convertToLong(tokenId).toString(),
            flowOrderConverter.convert(status),
            maker,
            origin,
            start?.let { OffsetDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) },
            end?.let { OffsetDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) },
            currencyAddress,
            continuation,
            size
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getOrderBidsByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val result = orderControllerApi.getOrderBidsByMaker(
            maker,
            flowOrderConverter.convert(status),
            origin,
            start?.let { OffsetDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) },
            end?.let { OffsetDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) },
            continuation,
            size
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getSellCurrencies(contract: String, tokenId: String): List<AssetTypeDto> {
        UnionConverter.convertToLong(tokenId) // Validation
        val assets = orderControllerApi.getSellCurrencies("$contract:$tokenId")
            .collectList().awaitFirst()
        return assets.map { FlowConverter.convert(it, blockchain).type }
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val result = orderControllerApi.getSellOrders(
            origin,
            continuation,
            size
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val result = orderControllerApi.getSellOrdersByCollection(
            collection,
            origin,
            continuation,
            size
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getSellOrdersByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencyId: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val result = orderControllerApi.getSellOrdersByItemAndByStatus(
            contract,
            UnionConverter.convertToLong(tokenId).toString(),
            maker,
            origin,
            continuation,
            size,
            flowOrderConverter.convert(status),
            currencyId
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val result = orderControllerApi.getSellOrdersByMaker(
            maker,
            origin,
            continuation,
            size
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }
}
