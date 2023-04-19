package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.AmmOrderDto
import com.rarible.protocol.dto.AmmOrderNftUpdateEventDto
import com.rarible.protocol.dto.CryptoPunkOrderDto
import com.rarible.protocol.dto.LegacyOrderDto
import com.rarible.protocol.dto.LooksRareOrderDto
import com.rarible.protocol.dto.OpenSeaV1OrderDto
import com.rarible.protocol.dto.OrderBasicSeaportDataV1Dto
import com.rarible.protocol.dto.OrderCancelDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.OrderExchangeHistoryDto
import com.rarible.protocol.dto.OrderOpenSeaV1DataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV2Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV3BuyDto
import com.rarible.protocol.dto.OrderRaribleV2DataV3SellDto
import com.rarible.protocol.dto.OrderSideDto
import com.rarible.protocol.dto.OrderSideMatchDto
import com.rarible.protocol.dto.OrderSudoSwapAmmDataV1Dto
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.dto.SeaportConsiderationDto
import com.rarible.protocol.dto.SeaportItemTypeDto
import com.rarible.protocol.dto.SeaportOfferDto
import com.rarible.protocol.dto.SeaportOrderTypeDto
import com.rarible.protocol.dto.SeaportV1OrderDto
import com.rarible.protocol.dto.X2Y2OrderDto
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.core.model.UnionPoolNftUpdateEvent
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.core.util.evalMakePrice
import com.rarible.protocol.union.core.util.evalTakePrice
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthLooksRareOrderDataV1Dto
import com.rarible.protocol.union.dto.EthOrderBasicSeaportDataV1Dto
import com.rarible.protocol.union.dto.EthOrderCryptoPunksDataDto
import com.rarible.protocol.union.dto.EthOrderDataLegacyDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV3BuyDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV3SellDto
import com.rarible.protocol.union.dto.EthOrderOpenSeaV1DataV1Dto
import com.rarible.protocol.union.dto.EthSeaportConsiderationDto
import com.rarible.protocol.union.dto.EthSeaportItemTypeDto
import com.rarible.protocol.union.dto.EthSeaportOfferDto
import com.rarible.protocol.union.dto.EthSeaportOrderTypeDto
import com.rarible.protocol.union.dto.EthSudoSwapAmmDataV1Dto
import com.rarible.protocol.union.dto.EthX2Y2OrderDataV1Dto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OnChainAmmOrderDto
import com.rarible.protocol.union.dto.OnChainOrderDto
import com.rarible.protocol.union.dto.OrderDataDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PendingOrderCancelDto
import com.rarible.protocol.union.dto.PendingOrderDto
import com.rarible.protocol.union.dto.PendingOrderMatchDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SudoSwapCurveTypeDto
import com.rarible.protocol.union.dto.SudoSwapPoolTypeDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.time.Instant

@Component
class EthOrderConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(order: com.rarible.protocol.dto.OrderDto, blockchain: BlockchainDto): OrderDto {
        try {
            return convertInternal(order, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Order: {} \n{}", blockchain, e.message, order)
            throw e
        }
    }

    suspend fun convert(event: OrderEventDto, blockchain: BlockchainDto): UnionOrderEvent {
        val eventTimeMarks = EthConverter.convert(event.eventTimeMarks)
        return when (event) {
            is OrderUpdateEventDto -> {
                val order = convert(event.order, blockchain)
                UnionOrderUpdateEvent(order, eventTimeMarks)
            }

            is AmmOrderNftUpdateEventDto -> {
                val orderId = OrderIdDto(blockchain, event.orderId)
                val include = event.inNft.map { ItemIdDto(blockchain, it) }
                val exclude = event.outNft.map { ItemIdDto(blockchain, it) }
                UnionPoolNftUpdateEvent(orderId, include.toSet(), exclude.toSet(), eventTimeMarks)
            }
        }
    }

    private suspend fun convertInternal(order: com.rarible.protocol.dto.OrderDto, blockchain: BlockchainDto): OrderDto {
        val orderId = OrderIdDto(blockchain, order.id ?: EthConverter.convert(order.hash))
        val maker = EthConverter.convert(order.maker, blockchain)
        val ethTaker = if (order.taker != null && order.taker != Address.ZERO()) order.taker else null
        val taker = ethTaker?.let { EthConverter.convert(it, blockchain) }
        val make = EthConverter.convertLegacy(order.make, blockchain)
        val take = EthConverter.convertLegacy(order.take, blockchain)
        // For BID (make = currency, take - NFT) we're calculating prices for taker
        val takePrice = evalTakePrice(make, take)
        // For SELL (make = NFT, take - currency) we're calculating prices for maker
        val makePrice = evalMakePrice(make, take)
        // So for USD conversion we are using take.type for MAKE price and vice versa
        val makePriceUsd = currencyService.toUsd(blockchain, take.type, makePrice)
        val takePriceUsd = currencyService.toUsd(blockchain, make.type, takePrice)
        val salt = EthConverter.convert(order.salt)
        val startedAt = order.start?.let { Instant.ofEpochSecond(it) }
        val endedAt = order.end?.let { Instant.ofEpochSecond(it) }
        val signature = order.signature?.let { EthConverter.convert(it) }
        val pending = order.pending?.map { convert(it, blockchain) }
        val status = convert(order.status!!) // TODO ETHEREUM should be required
        // By default, we need pay royalties
        val optionalRoyalties = order.optionalRoyalties ?: false
        return when (order) {
            is LegacyOrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.RARIBLE,
                    status = status,
                    maker = maker,
                    taker = taker,
                    make = make,
                    take = take,
                    salt = salt,
                    signature = signature,
                    pending = pending,
                    fill = order.fillValue!!,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    dbUpdatedAt = order.dbUpdatedAt,
                    makePrice = makePrice,
                    takePrice = takePrice,
                    makePriceUsd = makePriceUsd,
                    takePriceUsd = takePriceUsd,
                    data = EthOrderDataLegacyDto(
                        fee = order.data.fee.toBigInteger()
                    ),
                    optionalRoyalties = optionalRoyalties,
                )
            }
            is RaribleV2OrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.RARIBLE,
                    status = status,
                    maker = maker,
                    taker = taker,
                    make = make,
                    take = take,
                    salt = salt,
                    signature = signature,
                    pending = pending,
                    fill = order.fillValue!!,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    dbUpdatedAt = order.dbUpdatedAt,
                    makePrice = makePrice,
                    takePrice = takePrice,
                    makePriceUsd = makePriceUsd,
                    takePriceUsd = takePriceUsd,
                    data = convert(order.data, blockchain),
                    optionalRoyalties = optionalRoyalties,
                )
            }
            is OpenSeaV1OrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.OPEN_SEA,
                    status = status,
                    maker = maker,
                    taker = taker,
                    make = make,
                    take = take,
                    salt = salt,
                    signature = signature,
                    pending = pending,
                    fill = order.fillValue!!,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    dbUpdatedAt = order.dbUpdatedAt,
                    makePrice = makePrice,
                    takePrice = takePrice,
                    makePriceUsd = makePriceUsd,
                    takePriceUsd = takePriceUsd,
                    data = EthOrderOpenSeaV1DataV1Dto(
                        exchange = EthConverter.convert(order.data.exchange, blockchain),
                        makerRelayerFee = order.data.makerRelayerFee,
                        takerRelayerFee = order.data.takerRelayerFee,
                        makerProtocolFee = order.data.makerProtocolFee,
                        takerProtocolFee = order.data.takerProtocolFee,
                        feeRecipient = EthConverter.convert(order.data.feeRecipient, blockchain),
                        feeMethod = convert(order.data.feeMethod),
                        side = convert(order.data.side),
                        saleKind = convert(order.data.saleKind),
                        howToCall = convert(order.data.howToCall),
                        callData = EthConverter.convert(order.data.callData),
                        replacementPattern = EthConverter.convert(order.data.replacementPattern),
                        staticTarget = EthConverter.convert(order.data.staticTarget, blockchain),
                        staticExtraData = EthConverter.convert(order.data.staticExtraData),
                        extra = order.data.extra
                    ),
                    optionalRoyalties = optionalRoyalties,
                )
            }
            is CryptoPunkOrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.CRYPTO_PUNKS,
                    status = status,
                    maker = maker,
                    taker = taker,
                    make = make,
                    take = take,
                    salt = salt,
                    signature = signature,
                    pending = pending,
                    fill = order.fillValue!!,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    dbUpdatedAt = order.dbUpdatedAt,
                    makePrice = makePrice,
                    takePrice = takePrice,
                    makePriceUsd = makePriceUsd,
                    takePriceUsd = takePriceUsd,
                    data = EthOrderCryptoPunksDataDto(),
                    optionalRoyalties = optionalRoyalties,
                )
            }
            is SeaportV1OrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.OPEN_SEA,
                    status = status,
                    maker = maker,
                    taker = taker,
                    make = make,
                    take = take,
                    salt = salt,
                    signature = signature,
                    pending = pending,
                    fill = order.fillValue!!,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    dbUpdatedAt = order.dbUpdatedAt,
                    makePrice = makePrice,
                    takePrice = takePrice,
                    makePriceUsd = makePriceUsd,
                    takePriceUsd = takePriceUsd,
                    data = when (val data = order.data) {
                        is OrderBasicSeaportDataV1Dto -> EthOrderBasicSeaportDataV1Dto(
                            protocol = EthConverter.convert(data.protocol, blockchain),
                            orderType = convert(data.orderType),
                            offer = data.offer.map { convert(it, blockchain) },
                            consideration = data.consideration.map { convert(it, blockchain) },
                            zone = EthConverter.convert(data.zone, blockchain),
                            zoneHash = EthConverter.convert(data.zoneHash),
                            conduitKey = EthConverter.convert(data.conduitKey),
                            counter = data.counter,
                            nonce = data.nonce
                        )
                    },
                    optionalRoyalties = optionalRoyalties,
                )
            }
            is X2Y2OrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.X2Y2,
                    status = status,
                    maker = maker,
                    taker = taker,
                    make = make,
                    take = take,
                    salt = salt,
                    signature = signature,
                    pending = pending,
                    fill = order.fillValue!!,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    dbUpdatedAt = order.dbUpdatedAt,
                    makePrice = makePrice,
                    takePrice = takePrice,
                    makePriceUsd = makePriceUsd,
                    takePriceUsd = takePriceUsd,
                    data = EthX2Y2OrderDataV1Dto(
                        itemHash = order.data.itemHash.prefixed(),
                        orderId = order.data.orderId,
                        isCollectionOffer = order.data.isCollectionOffer,
                        isBundle = order.data.isBundle,
                        side = order.data.side
                    ),
                    optionalRoyalties = optionalRoyalties,
                )
            }
            is LooksRareOrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.LOOKSRARE,
                    status = status,
                    maker = maker,
                    taker = taker,
                    make = make,
                    take = take,
                    salt = salt,
                    signature = signature,
                    pending = pending,
                    fill = order.fillValue!!,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    dbUpdatedAt = order.dbUpdatedAt,
                    makePrice = makePrice,
                    takePrice = takePrice,
                    makePriceUsd = makePriceUsd,
                    takePriceUsd = takePriceUsd,
                    data = EthLooksRareOrderDataV1Dto(
                        minPercentageToAsk = order.data.minPercentageToAsk,
                        nonce = order.data.nonce,
                        strategy = EthConverter.convert(order.data.strategy, blockchain),
                        params = order.data.params?.let { EthConverter.convert(it) }
                    ),
                    optionalRoyalties = optionalRoyalties,
                )
            }
            is AmmOrderDto -> {
                val (data, platform) = when (val data = order.data) {
                    is OrderSudoSwapAmmDataV1Dto -> {
                        EthSudoSwapAmmDataV1Dto(
                            poolAddress = EthConverter.convert(data.poolAddress, blockchain),
                            poolType = convert(data.poolType),
                            assetRecipient = EthConverter.convert(data.assetRecipient, blockchain),
                            bondingCurve = EthConverter.convert(data.bondingCurve, blockchain),
                            curveType = convert(data.curveType),
                            delta = data.delta,
                            fee = data.fee,
                            feeDecimal = data.feeDecimal,
                        ) to PlatformDto.SUDOSWAP
                    }
                }
                OrderDto(
                    id = orderId,
                    platform = platform,
                    status = status,
                    maker = maker,
                    taker = taker,
                    make = make,
                    take = take,
                    salt = salt,
                    signature = signature,
                    pending = pending,
                    fill = order.fillValue!!,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    dbUpdatedAt = order.dbUpdatedAt,
                    makePrice = makePrice,
                    takePrice = takePrice,
                    makePriceUsd = makePriceUsd,
                    takePriceUsd = takePriceUsd,
                    data = data,
                    optionalRoyalties = optionalRoyalties,
                )
            }
        }
    }

    fun convert(source: SeaportOrderTypeDto): EthSeaportOrderTypeDto {
        return when (source) {
            SeaportOrderTypeDto.FULL_OPEN -> EthSeaportOrderTypeDto.FULL_OPEN
            SeaportOrderTypeDto.PARTIAL_OPEN -> EthSeaportOrderTypeDto.PARTIAL_OPEN
            SeaportOrderTypeDto.FULL_RESTRICTED -> EthSeaportOrderTypeDto.FULL_RESTRICTED
            SeaportOrderTypeDto.PARTIAL_RESTRICTED -> EthSeaportOrderTypeDto.PARTIAL_RESTRICTED
        }
    }

    fun convert(source: SeaportItemTypeDto): EthSeaportItemTypeDto {
        return when (source) {
            SeaportItemTypeDto.NATIVE -> EthSeaportItemTypeDto.NATIVE
            SeaportItemTypeDto.ERC20 -> EthSeaportItemTypeDto.ERC20
            SeaportItemTypeDto.ERC721 -> EthSeaportItemTypeDto.ERC721
            SeaportItemTypeDto.ERC1155 -> EthSeaportItemTypeDto.ERC1155
            SeaportItemTypeDto.ERC721_WITH_CRITERIA -> EthSeaportItemTypeDto.ERC721_WITH_CRITERIA
            SeaportItemTypeDto.ERC1155_WITH_CRITERIA -> EthSeaportItemTypeDto.ERC1155_WITH_CRITERIA
        }
    }

    fun convert(source: SeaportOfferDto, blockchain: BlockchainDto): EthSeaportOfferDto {
        return EthSeaportOfferDto(
            itemType = convert(source.itemType),
            token = EthConverter.convert(source.token, blockchain),
            identifierOrCriteria = source.identifierOrCriteria,
            startAmount = source.startAmount,
            endAmount = source.endAmount
        )
    }

    fun convert(source: SeaportConsiderationDto, blockchain: BlockchainDto): EthSeaportConsiderationDto {
        return EthSeaportConsiderationDto(
            itemType = convert(source.itemType),
            token = EthConverter.convert(source.token, blockchain),
            identifierOrCriteria = source.identifierOrCriteria,
            startAmount = source.startAmount,
            endAmount = source.endAmount,
            recipient = EthConverter.convert(source.recipient, blockchain)
        )
    }

    fun convert(source: com.rarible.protocol.dto.OrderRaribleV2DataDto, blockchain: BlockchainDto): OrderDataDto {
        return when(source) {
            is OrderRaribleV2DataV2Dto -> {
                EthOrderDataRaribleV2DataV1Dto(
                    payouts = source.payouts.map { EthConverter.convertToPayout(it, blockchain) },
                    originFees = source.originFees.map { EthConverter.convertToPayout(it, blockchain) }
                )
            }
            is OrderRaribleV2DataV1Dto -> {
                EthOrderDataRaribleV2DataV1Dto(
                    payouts = source.payouts.map { EthConverter.convertToPayout(it, blockchain) },
                    originFees = source.originFees.map { EthConverter.convertToPayout(it, blockchain) }
                )
            }
            is OrderRaribleV2DataV3SellDto -> {
                EthOrderDataRaribleV2DataV3SellDto(
                    payout = source.payout?.let { EthConverter.convertToPayout(it, blockchain) },
                    originFeeFirst = source.originFeeFirst?.let { EthConverter.convertToPayout(it, blockchain) },
                    originFeeSecond = source.originFeeSecond?.let { EthConverter.convertToPayout(it, blockchain) },
                    maxFeesBasePoint = source.maxFeesBasePoint,
                    marketplaceMarker = source.marketplaceMarker?.let { EthConverter.convert(it) }
                )
            }
            is OrderRaribleV2DataV3BuyDto -> {
                EthOrderDataRaribleV2DataV3BuyDto(
                    payout = source.payout?.let { EthConverter.convertToPayout(it, blockchain) },
                    originFeeFirst = source.originFeeFirst?.let { EthConverter.convertToPayout(it, blockchain) },
                    originFeeSecond = source.originFeeSecond?.let { EthConverter.convertToPayout(it, blockchain) },
                    marketplaceMarker = source.marketplaceMarker?.let { EthConverter.convert(it) }
                )
            }
        }
    }

    suspend fun convert(source: OrdersPaginationDto, blockchain: BlockchainDto): Slice<OrderDto> {
        return Slice(
            continuation = source.continuation,
            entities = source.orders.map { convert(it, blockchain) }
        )
    }

    fun convert(source: OrderStatusDto): com.rarible.protocol.dto.OrderStatusDto {
        return when (source) {
            OrderStatusDto.ACTIVE -> com.rarible.protocol.dto.OrderStatusDto.ACTIVE
            OrderStatusDto.FILLED -> com.rarible.protocol.dto.OrderStatusDto.FILLED
            OrderStatusDto.HISTORICAL -> com.rarible.protocol.dto.OrderStatusDto.HISTORICAL
            OrderStatusDto.INACTIVE -> com.rarible.protocol.dto.OrderStatusDto.INACTIVE
            OrderStatusDto.CANCELLED -> com.rarible.protocol.dto.OrderStatusDto.CANCELLED
        }
    }

    fun convert(source: com.rarible.protocol.dto.OrderStatusDto): OrderStatusDto {
        return when (source) {
            com.rarible.protocol.dto.OrderStatusDto.ACTIVE -> OrderStatusDto.ACTIVE
            com.rarible.protocol.dto.OrderStatusDto.FILLED -> OrderStatusDto.FILLED
            com.rarible.protocol.dto.OrderStatusDto.HISTORICAL -> OrderStatusDto.HISTORICAL
            com.rarible.protocol.dto.OrderStatusDto.INACTIVE -> OrderStatusDto.INACTIVE
            com.rarible.protocol.dto.OrderStatusDto.CANCELLED -> OrderStatusDto.CANCELLED
        }
    }

    fun convert(source: OrderSortDto?): com.rarible.protocol.dto.OrderSortDto? {
        return when (source) {
            OrderSortDto.LAST_UPDATE_ASC -> com.rarible.protocol.dto.OrderSortDto.LAST_UPDATE_ASC
            OrderSortDto.LAST_UPDATE_DESC -> com.rarible.protocol.dto.OrderSortDto.LAST_UPDATE_DESC
            else -> null
        }
    }

    fun convert(source: SyncSortDto?): com.rarible.protocol.dto.SyncSortDto? {
        return when (source) {
            SyncSortDto.DB_UPDATE_ASC -> com.rarible.protocol.dto.SyncSortDto.DB_UPDATE_ASC
            SyncSortDto.DB_UPDATE_DESC -> com.rarible.protocol.dto.SyncSortDto.DB_UPDATE_DESC
            else -> null
        }
    }

    fun convert(source: List<OrderStatusDto>?): List<com.rarible.protocol.dto.OrderStatusDto>? {
        return source?.map { convert(it) } ?: emptyList()
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.FeeMethod): EthOrderOpenSeaV1DataV1Dto.FeeMethod {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.FeeMethod.PROTOCOL_FEE -> EthOrderOpenSeaV1DataV1Dto.FeeMethod.PROTOCOL_FEE
            OrderOpenSeaV1DataV1Dto.FeeMethod.SPLIT_FEE -> EthOrderOpenSeaV1DataV1Dto.FeeMethod.SPLIT_FEE
        }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.Side): EthOrderOpenSeaV1DataV1Dto.Side {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.Side.SELL -> EthOrderOpenSeaV1DataV1Dto.Side.SELL
            OrderOpenSeaV1DataV1Dto.Side.BUY -> EthOrderOpenSeaV1DataV1Dto.Side.BUY
        }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.SaleKind): EthOrderOpenSeaV1DataV1Dto.SaleKind {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.SaleKind.FIXED_PRICE -> EthOrderOpenSeaV1DataV1Dto.SaleKind.FIXED_PRICE
            OrderOpenSeaV1DataV1Dto.SaleKind.DUTCH_AUCTION -> EthOrderOpenSeaV1DataV1Dto.SaleKind.DUTCH_AUCTION
        }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.HowToCall): EthOrderOpenSeaV1DataV1Dto.HowToCall {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.HowToCall.CALL -> EthOrderOpenSeaV1DataV1Dto.HowToCall.CALL
            OrderOpenSeaV1DataV1Dto.HowToCall.DELEGATE_CALL -> EthOrderOpenSeaV1DataV1Dto.HowToCall.DELEGATE_CALL
        }
    }


    private fun convert(source: OrderSideDto): PendingOrderMatchDto.Side {
        return when (source) {
            OrderSideDto.RIGHT -> PendingOrderMatchDto.Side.RIGHT
            OrderSideDto.LEFT -> PendingOrderMatchDto.Side.LEFT
        }
    }

    private fun convert(source: com.rarible.protocol.dto.SudoSwapCurveTypeDto): SudoSwapCurveTypeDto {
        return when (source) {
            com.rarible.protocol.dto.SudoSwapCurveTypeDto.LINEAR -> SudoSwapCurveTypeDto.LINEAR
            com.rarible.protocol.dto.SudoSwapCurveTypeDto.EXPONENTIAL -> SudoSwapCurveTypeDto.EXPONENTIAL
            com.rarible.protocol.dto.SudoSwapCurveTypeDto.UNKNOWN -> SudoSwapCurveTypeDto.UNKNOWN
        }
    }

    private fun convert(source: com.rarible.protocol.dto.SudoSwapPoolTypeDto): SudoSwapPoolTypeDto {
        return when (source) {
            com.rarible.protocol.dto.SudoSwapPoolTypeDto.TOKEN -> SudoSwapPoolTypeDto.TOKEN
            com.rarible.protocol.dto.SudoSwapPoolTypeDto.NFT -> SudoSwapPoolTypeDto.NFT
            com.rarible.protocol.dto.SudoSwapPoolTypeDto.TRADE -> SudoSwapPoolTypeDto.TRADE
        }
    }

    private fun convert(source: OrderExchangeHistoryDto, blockchain: BlockchainDto): PendingOrderDto {
        return when (source) {
            is OrderSideMatchDto -> PendingOrderMatchDto(
                id = OrderIdDto(blockchain, EthConverter.convert(source.hash)),
                make = source.make?.let { EthConverter.convertLegacy(it, blockchain) },
                take = source.take?.let { EthConverter.convertLegacy(it, blockchain) },
                date = source.date,
                side = source.side?.let { convert(it) },
                /** TODO ETHEREUM [OrderSideMatchDto.fill] must be BigDecimal, or fillValue */
                fill = source.fill.toBigDecimal(),
                maker = source.maker?.let { EthConverter.convert(it, blockchain) },
                taker = source.taker?.let { EthConverter.convert(it, blockchain) },
                counterHash = source.counterHash?.let { EthConverter.convert(it) },
                makeUsd = source.makeUsd,
                takeUsd = source.takeUsd,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd
            )
            is OrderCancelDto -> PendingOrderCancelDto(
                id = OrderIdDto(blockchain, EthConverter.convert(source.hash)),
                make = source.make?.let { EthConverter.convertLegacy(it, blockchain) },
                take = source.take?.let { EthConverter.convertLegacy(it, blockchain) },
                date = source.date,
                maker = source.maker?.let { EthConverter.convert(it, blockchain) },
                owner = source.owner?.let { EthConverter.convert(it, blockchain) }
            )
            is com.rarible.protocol.dto.OnChainOrderDto -> OnChainOrderDto(
                id = OrderIdDto(blockchain, EthConverter.convert(source.hash)),
                make = source.make?.let { EthConverter.convertLegacy(it, blockchain) },
                take = source.take?.let { EthConverter.convertLegacy(it, blockchain) },
                date = source.date,
                maker = source.maker?.let { EthConverter.convert(it, blockchain) }
            )
            is com.rarible.protocol.dto.OnChainAmmOrderDto -> OnChainAmmOrderDto(
                id = OrderIdDto(blockchain, EthConverter.convert(source.hash)),
                make = source.make?.let { EthConverter.convertLegacy(it, blockchain) },
                take = source.take?.let { EthConverter.convertLegacy(it, blockchain) },
                date = source.date,
                maker = source.maker?.let { EthConverter.convert(it, blockchain) }
            )
        }
    }
}

