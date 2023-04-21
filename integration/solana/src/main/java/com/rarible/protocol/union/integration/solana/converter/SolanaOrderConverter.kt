package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.solana.dto.AuctionHouseOrderDataV1Dto
import com.rarible.protocol.solana.dto.AuctionHouseOrderDto
import com.rarible.protocol.solana.dto.OrderDto
import com.rarible.protocol.solana.dto.OrdersDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.core.util.evalMakePrice
import com.rarible.protocol.union.core.util.evalTakePrice
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SolanaAuctionHouseDataV1Dto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class SolanaOrderConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(order: OrderDto, blockchain: BlockchainDto): UnionOrder {
        try {
            return convertToUnion(order, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Order: {} \n{}", blockchain, e.message, order)
            throw e
        }
    }

    private suspend fun convertToUnion(order: OrderDto, blockchain: BlockchainDto): UnionOrder {
        val make = SolanaConverter.convert(order.make, blockchain)
        val take = SolanaConverter.convert(order.take, blockchain)

        // For BID (make = currency, take - NFT) we're calculating prices for taker
        val takePrice = evalTakePrice(make, take)
        // For SELL (make = NFT, take - currency) we're calculating prices for maker
        val makePrice = evalMakePrice(make, take)
        // So for USD conversion we are using take.type for MAKE price and vice versa
        val makePriceUsd = currencyService.toUsd(blockchain, take.type, makePrice)
        val takePriceUsd = currencyService.toUsd(blockchain, make.type, takePrice)

        return UnionOrder(
            id = OrderIdDto(blockchain, order.hash),
            maker = UnionAddressConverter.convert(blockchain, order.maker),
            make = make,
            take = take,
            taker = null,
            fill = order.fill,
            platform = PlatformDto.RARIBLE,
            status = convert(order.status),
            startedAt = order.start,
            endedAt = order.end,
            cancelled = order.status == com.rarible.protocol.solana.dto.OrderStatusDto.CANCELLED,
            createdAt = order.createdAt,
            lastUpdatedAt = order.updatedAt,

            salt = "",
            signature = null,
            pending = emptyList(),

            makeStock = order.makeStock ?: BigDecimal.ZERO, // TODO make it required later
            makePrice = makePrice,
            takePrice = takePrice,
            makePriceUsd = makePriceUsd,
            takePriceUsd = takePriceUsd,

            data = convertOrderData(order, blockchain)
        )
    }

    private fun convertOrderData(
        order: OrderDto,
        blockchain: BlockchainDto
    ) = when (order) {
        is AuctionHouseOrderDto -> when (val orderData = order.data) {
            is AuctionHouseOrderDataV1Dto -> SolanaAuctionHouseDataV1Dto(
                fee = order.auctionHouseFee,
                requiresSignOff = order.auctionHouseRequiresSignOff,
                auctionHouse = ContractAddress(blockchain, orderData.auctionHouse)
            )
        }
    }

    private fun convert(status: com.rarible.protocol.solana.dto.OrderStatusDto): UnionOrder.Status = when (status) {
        com.rarible.protocol.solana.dto.OrderStatusDto.ACTIVE -> UnionOrder.Status.ACTIVE
        com.rarible.protocol.solana.dto.OrderStatusDto.FILLED -> UnionOrder.Status.FILLED
        com.rarible.protocol.solana.dto.OrderStatusDto.CANCELLED -> UnionOrder.Status.CANCELLED
        com.rarible.protocol.solana.dto.OrderStatusDto.INACTIVE -> UnionOrder.Status.INACTIVE
    }

    suspend fun convert(source: OrdersDto, blockchain: BlockchainDto): Slice<UnionOrder> = Slice(
        continuation = source.continuation,
        entities = source.orders.map { convert(it, blockchain) }
    )

    fun convert(source: SyncSortDto?): com.rarible.protocol.solana.dto.SyncSortDto? {
        return when (source) {
            SyncSortDto.DB_UPDATE_ASC -> com.rarible.protocol.solana.dto.SyncSortDto.DB_UPDATE_ASC
            SyncSortDto.DB_UPDATE_DESC -> com.rarible.protocol.solana.dto.SyncSortDto.DB_UPDATE_DESC
            else -> null
        }
    }

    fun convert(source: OrderSortDto?): com.rarible.protocol.solana.dto.OrderSortDto? {
        return when (source) {
            OrderSortDto.LAST_UPDATE_ASC -> com.rarible.protocol.solana.dto.OrderSortDto.LAST_UPDATE_ASC
            OrderSortDto.LAST_UPDATE_DESC -> com.rarible.protocol.solana.dto.OrderSortDto.LAST_UPDATE_DESC
            else -> null
        }
    }

    fun convert(source: List<OrderStatusDto>?): List<com.rarible.protocol.solana.dto.OrderStatusDto>? =
        source?.map { convert(it) } ?: emptyList()

    fun convert(source: OrderStatusDto): com.rarible.protocol.solana.dto.OrderStatusDto = when (source) {
        OrderStatusDto.ACTIVE -> com.rarible.protocol.solana.dto.OrderStatusDto.ACTIVE
        OrderStatusDto.FILLED -> com.rarible.protocol.solana.dto.OrderStatusDto.FILLED
        OrderStatusDto.HISTORICAL, // TODO[orders]: discuss.
        OrderStatusDto.INACTIVE,
        OrderStatusDto.CANCELLED -> com.rarible.protocol.solana.dto.OrderStatusDto.CANCELLED
    }
}
