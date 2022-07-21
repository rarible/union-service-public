package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.dto.OrderBasicSeaportDataV1Dto
import com.rarible.protocol.dto.OrderCancelDto
import com.rarible.protocol.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.dto.OrderSideMatchDto
import com.rarible.protocol.dto.SeaportItemTypeDto
import com.rarible.protocol.dto.SeaportOrderTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV3BuyDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV3SellDto
import com.rarible.protocol.union.dto.EthOrderOpenSeaV1DataV1Dto
import com.rarible.protocol.union.dto.EthOrderSeaportDataV1Dto
import com.rarible.protocol.union.dto.EthSeaportItemTypeDto
import com.rarible.protocol.union.dto.EthSeaportOrderTypeDto
import com.rarible.protocol.union.dto.OnChainOrderDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PendingOrderCancelDto
import com.rarible.protocol.union.dto.PendingOrderMatchDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCryptoPunksOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOnChainOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOpenSeaV1OrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderBasicSeaportDataV1Dto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderCancelDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderDataRaribleV2DataV3BuyDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderDataRaribleV2DataV3SellDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderSideMatchDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthSeaportConsideration
import com.rarible.protocol.union.integration.ethereum.data.randomEthSeaportOffer
import com.rarible.protocol.union.integration.ethereum.data.randomEthSeaportV1OrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EthOrderConverterTest {

    private val ethOrderConverter = EthOrderConverter(CurrencyMock.currencyServiceMock)

    @Test
    fun `eth order - legacy`() = runBlocking<Unit> {
        val dto = randomEthSellOrderDto().copy(taker = randomAddress())

        val converted = ethOrderConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.status.name).isEqualTo(dto.status!!.name)
        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.platform).isEqualTo(PlatformDto.RARIBLE)
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.taker!!.value).isEqualTo(dto.taker!!.prefixed())
        assertThat(converted.make.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make.value).isEqualTo(dto.make.valueDecimal)
        assertThat(converted.take.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take.value).isEqualTo(dto.take.valueDecimal)
        assertThat(converted.salt).isEqualTo(dto.salt.prefixed())
        assertThat(converted.signature).isEqualTo(dto.signature!!.prefixed())
        assertThat(converted.fill).isEqualTo(dto.fillValue)
        assertThat(converted.startedAt!!.epochSecond).isEqualTo(dto.start)
        assertThat(converted.endedAt!!.epochSecond).isEqualTo(dto.end)
        assertThat(converted.makeStock).isEqualTo(dto.makeStockValue)
        assertThat(converted.cancelled).isEqualTo(dto.cancelled)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdateAt)
        assertThat(converted.makePrice).isEqualTo(
            dto.take.valueDecimal!!.setScale(18) / dto.make.valueDecimal!!.setScale(18)
        )
        assertThat(converted.takePrice).isNull()
        // In mock we are converting price 1 to 1
        assertThat(converted.makePriceUsd).isEqualTo(
            dto.take.valueDecimal!!.setScale(18) / dto.make.valueDecimal!!.setScale(18)
        )
        assertThat(converted.takePriceUsd).isNull()
    }

    @Test
    fun `eth order pending - side match`() = runBlocking<Unit> {
        val order = randomEthSellOrderDto().copy(pending = listOf(randomEthOrderSideMatchDto()))
        val dto = order.pending!![0] as OrderSideMatchDto

        val converted = ethOrderConverter.convert(order, BlockchainDto.ETHEREUM)
            .pending!![0] as PendingOrderMatchDto

        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.maker!!.value).isEqualTo(dto.maker!!.prefixed())
        assertThat(converted.make!!.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make!!.value).isEqualTo(dto.make!!.valueDecimal)
        assertThat(converted.take!!.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take!!.value).isEqualTo(dto.take!!.valueDecimal)

        assertThat(converted.fill).isEqualTo(dto.fill.toBigDecimal())
        assertThat(converted.side!!.name).isEqualTo(dto.side!!.name)
        assertThat(converted.fill).isEqualTo(dto.fill.toBigDecimal())
        assertThat(converted.taker!!.value).isEqualTo(dto.taker!!.prefixed())
        assertThat(converted.counterHash).isEqualTo(dto.counterHash!!.prefixed())
        assertThat(converted.makeUsd).isEqualTo(dto.makeUsd)
        assertThat(converted.takeUsd).isEqualTo(dto.takeUsd)
        assertThat(converted.makePriceUsd).isEqualTo(dto.makePriceUsd)
        assertThat(converted.takePriceUsd).isEqualTo(dto.takePriceUsd)

    }

    @Test
    fun `eth order pending - cancel`() = runBlocking<Unit> {
        val order = randomEthSellOrderDto().copy(pending = listOf(randomEthOrderCancelDto()))
        val dto = order.pending!![0] as OrderCancelDto

        val converted = ethOrderConverter.convert(order, BlockchainDto.ETHEREUM)
            .pending!![0] as PendingOrderCancelDto

        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.maker!!.value).isEqualTo(dto.maker!!.prefixed())
        assertThat(converted.make!!.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make!!.value).isEqualTo(dto.make!!.valueDecimal)
        assertThat(converted.take!!.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take!!.value).isEqualTo(dto.take!!.valueDecimal)

        assertThat(converted.owner!!.value).isEqualTo(dto.owner!!.prefixed())
    }

    @Test
    fun `eth order pending - on chain`() = runBlocking<Unit> {
        val order = randomEthSellOrderDto().copy(pending = listOf(randomEthOnChainOrderDto()))
        val dto = order.pending!![0] as com.rarible.protocol.dto.OnChainOrderDto

        val converted = ethOrderConverter.convert(order, BlockchainDto.ETHEREUM)
            .pending!![0] as OnChainOrderDto

        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.maker!!.value).isEqualTo(dto.maker!!.prefixed())
        assertThat(converted.make!!.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make!!.value).isEqualTo(dto.make!!.valueDecimal)
        assertThat(converted.take!!.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take!!.value).isEqualTo(dto.take!!.valueDecimal)
    }

    @Test
    fun `eth order rarible v2`() = runBlocking<Unit> {
        val dto = randomEthV2OrderDto().copy(taker = randomAddress())

        val converted = ethOrderConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.status.name).isEqualTo(dto.status!!.name)
        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.platform).isEqualTo(PlatformDto.RARIBLE)
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.taker!!.value).isEqualTo(dto.taker!!.prefixed())
        assertThat(converted.make.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make.value).isEqualTo(dto.make.valueDecimal)
        assertThat(converted.take.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take.value).isEqualTo(dto.take.valueDecimal)
        assertThat(converted.salt).isEqualTo(dto.salt.prefixed())
        assertThat(converted.signature).isEqualTo(dto.signature!!.prefixed())
        assertThat(converted.fill).isEqualTo(dto.fillValue)
        assertThat(converted.startedAt!!.epochSecond).isEqualTo(dto.start)
        assertThat(converted.endedAt!!.epochSecond).isEqualTo(dto.end)
        assertThat(converted.makeStock).isEqualTo(dto.makeStockValue)
        assertThat(converted.cancelled).isEqualTo(dto.cancelled)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdateAt)
        assertThat(converted.makePrice).isEqualTo(
            dto.take.valueDecimal!!.setScale(18) / dto.make.valueDecimal!!.setScale(18)
        )
        assertThat(converted.takePrice).isNull()
        // In mock we are converting price 1 to 1
        assertThat(converted.makePriceUsd).isEqualTo(
            dto.take.valueDecimal!!.setScale(18) / dto.make.valueDecimal!!.setScale(18)
        )
        assertThat(converted.takePriceUsd).isNull()

        val data = converted.data as EthOrderDataRaribleV2DataV1Dto
        assertThat(data.payouts[0].account.value).isEqualTo((dto.data as OrderRaribleV2DataV1Dto).payouts[0].account.prefixed())
        assertThat(data.payouts[0].value).isEqualTo((dto.data as OrderRaribleV2DataV1Dto).payouts[0].value)

        assertThat(data.originFees[0].account.value).isEqualTo((dto.data as OrderRaribleV2DataV1Dto).originFees[0].account.prefixed())
        assertThat(data.originFees[0].value).isEqualTo((dto.data as OrderRaribleV2DataV1Dto).originFees[0].value)
    }

    @Test
    fun `eth order rarible v2 data v3 sell`() = runBlocking<Unit> {
        val dataDto = randomEthOrderDataRaribleV2DataV3SellDto()
        val orderDto = randomEthV2OrderDto().copy(data = dataDto)

        val converted = ethOrderConverter.convert(orderDto, BlockchainDto.ETHEREUM).data
        val data = converted as EthOrderDataRaribleV2DataV3SellDto

        assertThat(data.payout!!.account.value).isEqualTo(dataDto.payout?.account?.prefixed())
        assertThat(data.originFeeFirst!!.account.value).isEqualTo(dataDto.originFeeFirst?.account?.prefixed())
        assertThat(data.originFeeSecond!!.account?.value).isEqualTo(dataDto.originFeeSecond?.account?.prefixed())
        assertThat(data.maxFeesBasePoint).isEqualTo(dataDto.maxFeesBasePoint)
        assertThat(data.marketplaceMarker!!).isEqualTo(dataDto.marketplaceMarker?.prefixed())
    }

    @Test
    fun `eth order rarible v2 data v3 buy`() = runBlocking<Unit> {
        val dataDto = randomEthOrderDataRaribleV2DataV3BuyDto()
        val orderDto = randomEthV2OrderDto().copy(data = dataDto)

        val converted = ethOrderConverter.convert(orderDto, BlockchainDto.ETHEREUM).data
        val data = converted as EthOrderDataRaribleV2DataV3BuyDto

        assertThat(data.payout!!.account.value).isEqualTo(dataDto.payout?.account?.prefixed())
        assertThat(data.originFeeFirst!!.account.value).isEqualTo(dataDto.originFeeFirst?.account?.prefixed())
        assertThat(data.originFeeSecond!!.account?.value).isEqualTo(dataDto.originFeeSecond?.account?.prefixed())
        assertThat(data.marketplaceMarker!!).isEqualTo(dataDto.marketplaceMarker?.prefixed())
    }

    @Test
    fun `eth order opensea v1`() = runBlocking<Unit> {
        val dto = randomEthOpenSeaV1OrderDto().copy(taker = randomAddress())

        val converted = ethOrderConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.status.name).isEqualTo(dto.status!!.name)
        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.platform).isEqualTo(PlatformDto.OPEN_SEA)
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.taker!!.value).isEqualTo(dto.taker!!.prefixed())
        assertThat(converted.make.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make.value).isEqualTo(dto.make.valueDecimal)
        assertThat(converted.take.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take.value).isEqualTo(dto.take.valueDecimal)
        assertThat(converted.salt).isEqualTo(dto.salt.prefixed())
        assertThat(converted.signature).isEqualTo(dto.signature!!.prefixed())
        assertThat(converted.fill).isEqualTo(dto.fillValue)
        assertThat(converted.startedAt!!.epochSecond).isEqualTo(dto.start)
        assertThat(converted.endedAt!!.epochSecond).isEqualTo(dto.end)
        assertThat(converted.makeStock).isEqualTo(dto.makeStockValue)
        assertThat(converted.cancelled).isEqualTo(dto.cancelled)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdateAt)
        assertThat(converted.makePrice).isEqualTo(
            dto.take.valueDecimal!!.setScale(18) / dto.make.valueDecimal!!.setScale(18)
        )
        assertThat(converted.takePrice).isNull()
        // In mock we are converting price 1 to 1
        assertThat(converted.makePriceUsd).isEqualTo(
            dto.take.valueDecimal!!.setScale(18) / dto.make.valueDecimal!!.setScale(18)
        )
        assertThat(converted.takePriceUsd).isNull()
    }

    @Test
    fun `eth order opensea v1 - data`() = runBlocking<Unit> {
        val order = randomEthOpenSeaV1OrderDto()
        val dto = order.data

        val converted = ethOrderConverter.convert(order, BlockchainDto.ETHEREUM).data as EthOrderOpenSeaV1DataV1Dto

        assertThat(converted.exchange.value).isEqualTo(dto.exchange.prefixed())
        assertThat(converted.makerRelayerFee).isEqualTo(dto.makerRelayerFee)
        assertThat(converted.takerRelayerFee).isEqualTo(dto.takerRelayerFee)
        assertThat(converted.makerProtocolFee).isEqualTo(dto.makerProtocolFee)
        assertThat(converted.takerProtocolFee).isEqualTo(dto.takerProtocolFee)
        assertThat(converted.feeRecipient.value).isEqualTo(dto.feeRecipient.prefixed())
        assertThat(converted.feeMethod.name).isEqualTo(dto.feeMethod.name)
        assertThat(converted.side.name).isEqualTo(dto.side.name)
        assertThat(converted.saleKind.name).isEqualTo(dto.saleKind.name)
        assertThat(converted.howToCall.name).isEqualTo(dto.howToCall.name)
        assertThat(converted.callData).isEqualTo(dto.callData.prefixed())
        assertThat(converted.replacementPattern).isEqualTo(dto.replacementPattern.prefixed())
        assertThat(converted.staticTarget.value).isEqualTo(dto.staticTarget.prefixed())
        assertThat(converted.staticExtraData).isEqualTo(dto.staticExtraData.prefixed())
        assertThat(converted.extra).isEqualTo(dto.extra)
    }

    @Test
    fun `eth order seaport v1`() = runBlocking<Unit> {
        val dto = randomEthSeaportV1OrderDto().copy(taker = randomAddress())
        val converted = ethOrderConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.status.name).isEqualTo(dto.status!!.name)
        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.platform).isEqualTo(PlatformDto.OPEN_SEA)
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.taker!!.value).isEqualTo(dto.taker!!.prefixed())
        assertThat(converted.make.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make.value).isEqualTo(dto.make.valueDecimal)
        assertThat(converted.take.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take.value).isEqualTo(dto.take.valueDecimal)
        assertThat(converted.salt).isEqualTo(dto.salt.prefixed())
        assertThat(converted.signature).isEqualTo(dto.signature!!.prefixed())
        assertThat(converted.fill).isEqualTo(dto.fillValue)
        assertThat(converted.startedAt!!.epochSecond).isEqualTo(dto.start)
        assertThat(converted.endedAt!!.epochSecond).isEqualTo(dto.end)
        assertThat(converted.makeStock).isEqualTo(dto.makeStockValue)
        assertThat(converted.cancelled).isEqualTo(dto.cancelled)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdateAt)
        assertThat(converted.makePrice).isEqualTo(
            dto.take.valueDecimal!!.setScale(18) / dto.make.valueDecimal!!.setScale(18)
        )
        assertThat(converted.takePrice).isNull()
        // In mock we are converting price 1 to 1
        assertThat(converted.makePriceUsd).isEqualTo(
            dto.take.valueDecimal!!.setScale(18) / dto.make.valueDecimal!!.setScale(18)
        )
        assertThat(converted.takePriceUsd).isNull()
    }

    @Test
    fun `eth order seaport v1 - data`() = runBlocking<Unit> {
        val offerDto = randomEthSeaportOffer().copy(itemType = SeaportItemTypeDto.ERC721)
        val considerationDto = randomEthSeaportConsideration().copy(itemType = SeaportItemTypeDto.ERC20)
        val dto = randomEthOrderBasicSeaportDataV1Dto()
            .copy(
                orderType = SeaportOrderTypeDto.FULL_RESTRICTED,
                offer = listOf(offerDto),
                consideration = listOf(considerationDto)
            )
        val order = randomEthSeaportV1OrderDto().copy(data = dto)

        val converted = ethOrderConverter.convert(order, BlockchainDto.ETHEREUM).data as EthOrderSeaportDataV1Dto

        assertThat(converted.protocol.value).isEqualTo(dto.protocol.prefixed())
        assertThat(converted.orderType).isEqualTo(EthSeaportOrderTypeDto.FULL_RESTRICTED)
        assertThat(converted.zone.value).isEqualTo(dto.zone.prefixed())
        assertThat(converted.zoneHash).isEqualTo(dto.zoneHash.prefixed())
        assertThat(converted.conduitKey).isEqualTo(dto.conduitKey.prefixed())
        assertThat(converted.counter).isEqualTo(dto.counter)
        with(converted.offer.single()) {
            assertThat(itemType).isEqualTo(EthSeaportItemTypeDto.ERC721)
            assertThat(token.value).isEqualTo(offerDto.token.prefixed())
            assertThat(startAmount).isEqualTo(offerDto.startAmount)
            assertThat(endAmount).isEqualTo(offerDto.endAmount)
        }
        with(converted.consideration.single()) {
            assertThat(itemType).isEqualTo(EthSeaportItemTypeDto.ERC20)
            assertThat(token.value).isEqualTo(considerationDto.token.prefixed())
            assertThat(startAmount).isEqualTo(considerationDto.startAmount)
            assertThat(endAmount).isEqualTo(considerationDto.endAmount)
            assertThat(recipient.value).isEqualTo(considerationDto.recipient.prefixed())
        }
    }

    @Test
    fun `eth order crypto punks`() = runBlocking<Unit> {
        val dto = randomEthCryptoPunksOrderDto().copy(taker = randomAddress())

        val converted = ethOrderConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.status.name).isEqualTo(dto.status!!.name)
        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.platform).isEqualTo(PlatformDto.CRYPTO_PUNKS)
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.taker!!.value).isEqualTo(dto.taker!!.prefixed())
        assertThat(converted.make.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make.value).isEqualTo(dto.make.valueDecimal)
        assertThat(converted.take.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take.value).isEqualTo(dto.take.valueDecimal)
        assertThat(converted.salt).isEqualTo(dto.salt.prefixed())
        assertThat(converted.signature).isEqualTo(dto.signature!!.prefixed())
        assertThat(converted.fill).isEqualTo(dto.fillValue)
        assertThat(converted.startedAt!!.epochSecond).isEqualTo(dto.start)
        assertThat(converted.endedAt!!.epochSecond).isEqualTo(dto.end)
        assertThat(converted.makeStock).isEqualTo(dto.makeStockValue)
        assertThat(converted.cancelled).isEqualTo(dto.cancelled)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdateAt)
        assertThat(converted.makePrice).isEqualTo(
            dto.take.valueDecimal!!.setScale(18) / dto.make.valueDecimal!!.setScale(18)
        )
        assertThat(converted.takePrice).isNull()
        // In mock we are converting price 1 to 1
        assertThat(converted.makePriceUsd).isEqualTo(
            dto.take.valueDecimal!!.setScale(18) / dto.make.valueDecimal!!.setScale(18)
        )
        assertThat(converted.takePriceUsd).isNull()
    }

    @Test
    fun `eth order status`() {
        // given
        val unionStatuses = listOf(
            OrderStatusDto.ACTIVE,
            OrderStatusDto.FILLED,
            OrderStatusDto.INACTIVE,
            OrderStatusDto.HISTORICAL,
            OrderStatusDto.CANCELLED
        )
        val ethStatuses = listOf(
            com.rarible.protocol.dto.OrderStatusDto.ACTIVE,
            com.rarible.protocol.dto.OrderStatusDto.FILLED,
            com.rarible.protocol.dto.OrderStatusDto.INACTIVE,
            com.rarible.protocol.dto.OrderStatusDto.HISTORICAL,
            com.rarible.protocol.dto.OrderStatusDto.CANCELLED
        )

        // when
        val actual = ethStatuses.map { ethOrderConverter.convert(it) }

        // then
        assertThat(actual).isEqualTo(unionStatuses)
    }

    @Test
    fun `union order status`() {
        // given
        val ethStatuses = listOf(
            com.rarible.protocol.dto.OrderStatusDto.ACTIVE,
            com.rarible.protocol.dto.OrderStatusDto.FILLED,
            com.rarible.protocol.dto.OrderStatusDto.INACTIVE,
            com.rarible.protocol.dto.OrderStatusDto.INACTIVE,
            com.rarible.protocol.dto.OrderStatusDto.CANCELLED
        )
        val unionStatuses = listOf(
            OrderStatusDto.ACTIVE,
            OrderStatusDto.FILLED,
            OrderStatusDto.INACTIVE,
            OrderStatusDto.HISTORICAL,
            OrderStatusDto.CANCELLED
        )

        // when
        val actual = unionStatuses.map { ethOrderConverter.convert(it) }

        // then
        assertThat(actual).isEqualTo(ethStatuses)
    }
}
