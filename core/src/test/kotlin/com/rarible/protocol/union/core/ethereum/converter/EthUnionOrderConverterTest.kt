package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.OnChainOrderDto
import com.rarible.protocol.dto.OrderCancelDto
import com.rarible.protocol.dto.OrderSideMatchDto
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.test.data.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthUnionOrderConverterTest {

    @Test
    fun legacy() {
        val dto = randomEthLegacyOrderDto()

        val converted = EthUnionOrderConverter.convert(dto, EthBlockchainDto.ETHEREUM) as EthLegacyOrderDto

        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.taker!!.value).isEqualTo(dto.taker!!.prefixed())
        assertThat(converted.make.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        assertThat(converted.take.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        assertThat(converted.salt).isEqualTo(dto.salt.prefixed())
        assertThat(converted.signature).isEqualTo(dto.signature!!.prefixed())
        assertThat(converted.fill).isEqualTo(dto.fill)
        assertThat(converted.startedAt!!.epochSecond).isEqualTo(dto.start)
        assertThat(converted.endedAt!!.epochSecond).isEqualTo(dto.end)
        assertThat(converted.makeStock).isEqualTo(dto.makeStock)
        assertThat(converted.cancelled).isEqualTo(dto.cancelled)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdateAt)
        assertThat(converted.makePriceUsd).isEqualTo(dto.makePriceUsd)
        assertThat(converted.takePriceUsd).isEqualTo(dto.takePriceUsd)
        assertThat(converted.priceHistory[0].date).isEqualTo(dto.priceHistory!![0].date)
        assertThat(converted.priceHistory[0].makeValue).isEqualTo(dto.priceHistory!![0].makeValue)
        assertThat(converted.priceHistory[0].takeValue).isEqualTo(dto.priceHistory!![0].takeValue)
        assertThat(converted.data.fee).isEqualTo(dto.data.fee)
    }

    @Test
    fun `pending - side match`() {
        val order = randomEthLegacyOrderDto().copy(pending = listOf(randomEthOrderSideMatchDto()))
        val dto = order.pending!![0] as OrderSideMatchDto

        val converted = EthUnionOrderConverter.convert(order, EthBlockchainDto.ETHEREUM)
            .pending!![0] as EthPendingOrderMatchDto

        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.maker!!.value).isEqualTo(dto.maker!!.prefixed())
        assertThat(converted.make!!.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make!!.value).isEqualTo(dto.make!!.value)
        assertThat(converted.take!!.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take!!.value).isEqualTo(dto.take!!.value)

        assertThat(converted.fill).isEqualTo(dto.fill)
        assertThat(converted.side!!.name).isEqualTo(dto.side!!.name)
        assertThat(converted.fill).isEqualTo(dto.fill)
        assertThat(converted.taker!!.value).isEqualTo(dto.taker!!.prefixed())
        assertThat(converted.counterHash).isEqualTo(dto.counterHash!!.prefixed())
        assertThat(converted.makeUsd).isEqualTo(dto.makeUsd)
        assertThat(converted.takeUsd).isEqualTo(dto.takeUsd)
        assertThat(converted.makePriceUsd).isEqualTo(dto.makePriceUsd)
        assertThat(converted.takePriceUsd).isEqualTo(dto.takePriceUsd)

    }

    @Test
    fun `pending - cancel`() {
        val order = randomEthLegacyOrderDto().copy(pending = listOf(randomEthOrderCancelDto()))
        val dto = order.pending!![0] as OrderCancelDto

        val converted = EthUnionOrderConverter.convert(order, EthBlockchainDto.ETHEREUM)
            .pending!![0] as EthPendingOrderCancelDto

        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.maker!!.value).isEqualTo(dto.maker!!.prefixed())
        assertThat(converted.make!!.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make!!.value).isEqualTo(dto.make!!.value)
        assertThat(converted.take!!.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take!!.value).isEqualTo(dto.take!!.value)

        assertThat(converted.owner!!.value).isEqualTo(dto.owner!!.prefixed())
    }

    @Test
    fun `pending - on chain`() {
        val order = randomEthLegacyOrderDto().copy(pending = listOf(randomEthOnChainOrderDto()))
        val dto = order.pending!![0] as OnChainOrderDto

        val converted = EthUnionOrderConverter.convert(order, EthBlockchainDto.ETHEREUM)
            .pending!![0] as EthOnChainOrderDto

        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.maker!!.value).isEqualTo(dto.maker!!.prefixed())
        assertThat(converted.make!!.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make!!.value).isEqualTo(dto.make!!.value)
        assertThat(converted.take!!.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take!!.value).isEqualTo(dto.take!!.value)
    }

    @Test
    fun `rarible v2`() {
        val dto = randomEthV2OrderDto()

        val converted = EthUnionOrderConverter.convert(dto, EthBlockchainDto.ETHEREUM) as EthRaribleV2OrderDto

        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.taker!!.value).isEqualTo(dto.taker!!.prefixed())
        assertThat(converted.make.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        assertThat(converted.take.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        assertThat(converted.salt).isEqualTo(dto.salt.prefixed())
        assertThat(converted.signature).isEqualTo(dto.signature!!.prefixed())
        assertThat(converted.fill).isEqualTo(dto.fill)
        assertThat(converted.startedAt!!.epochSecond).isEqualTo(dto.start)
        assertThat(converted.endedAt!!.epochSecond).isEqualTo(dto.end)
        assertThat(converted.makeStock).isEqualTo(dto.makeStock)
        assertThat(converted.cancelled).isEqualTo(dto.cancelled)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdateAt)
        assertThat(converted.makePriceUsd).isEqualTo(dto.makePriceUsd)
        assertThat(converted.takePriceUsd).isEqualTo(dto.takePriceUsd)
        assertThat(converted.priceHistory[0].date).isEqualTo(dto.priceHistory!![0].date)
        assertThat(converted.priceHistory[0].makeValue).isEqualTo(dto.priceHistory!![0].makeValue)
        assertThat(converted.priceHistory[0].takeValue).isEqualTo(dto.priceHistory!![0].takeValue)

        assertThat(converted.data.payouts[0].account.value).isEqualTo(dto.data.payouts[0].account.prefixed())
        assertThat(converted.data.payouts[0].value).isEqualTo(dto.data.payouts[0].value)

        assertThat(converted.data.originFees[0].account.value).isEqualTo(dto.data.originFees[0].account.prefixed())
        assertThat(converted.data.originFees[0].value).isEqualTo(dto.data.originFees[0].value)
    }

    @Test
    fun `opensea v1`() {
        val dto = randomEthOpenSeaV1OrderDto()

        val converted = EthUnionOrderConverter.convert(dto, EthBlockchainDto.ETHEREUM) as EthOpenSeaV1OrderDto

        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.taker!!.value).isEqualTo(dto.taker!!.prefixed())
        assertThat(converted.make.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        assertThat(converted.take.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        assertThat(converted.salt).isEqualTo(dto.salt.prefixed())
        assertThat(converted.signature).isEqualTo(dto.signature!!.prefixed())
        assertThat(converted.fill).isEqualTo(dto.fill)
        assertThat(converted.startedAt!!.epochSecond).isEqualTo(dto.start)
        assertThat(converted.endedAt!!.epochSecond).isEqualTo(dto.end)
        assertThat(converted.makeStock).isEqualTo(dto.makeStock)
        assertThat(converted.cancelled).isEqualTo(dto.cancelled)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdateAt)
        assertThat(converted.makePriceUsd).isEqualTo(dto.makePriceUsd)
        assertThat(converted.takePriceUsd).isEqualTo(dto.takePriceUsd)
        assertThat(converted.priceHistory[0].date).isEqualTo(dto.priceHistory!![0].date)
        assertThat(converted.priceHistory[0].makeValue).isEqualTo(dto.priceHistory!![0].makeValue)
        assertThat(converted.priceHistory[0].takeValue).isEqualTo(dto.priceHistory!![0].takeValue)
    }

    @Test
    fun `opensea v1 - data`() {
        val order = randomEthOpenSeaV1OrderDto()
        val dto = order.data

        val converted = (EthUnionOrderConverter.convert(order, EthBlockchainDto.ETHEREUM) as EthOpenSeaV1OrderDto).data

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
    fun `crypto punks`() {
        val dto = randomEthCryptoPunksOrderDto()

        val converted = EthUnionOrderConverter.convert(dto, EthBlockchainDto.ETHEREUM) as EthCryptoPunksOrderDto

        assertThat(converted.id.value).isEqualTo(dto.hash.prefixed())
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.taker!!.value).isEqualTo(dto.taker!!.prefixed())
        assertThat(converted.make.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        assertThat(converted.take.type).isInstanceOf(EthErc20AssetTypeDto::class.java)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        assertThat(converted.salt).isEqualTo(dto.salt.prefixed())
        assertThat(converted.signature).isEqualTo(dto.signature!!.prefixed())
        assertThat(converted.fill).isEqualTo(dto.fill)
        assertThat(converted.startedAt!!.epochSecond).isEqualTo(dto.start)
        assertThat(converted.endedAt!!.epochSecond).isEqualTo(dto.end)
        assertThat(converted.makeStock).isEqualTo(dto.makeStock)
        assertThat(converted.cancelled).isEqualTo(dto.cancelled)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdateAt)
        assertThat(converted.makePriceUsd).isEqualTo(dto.makePriceUsd)
        assertThat(converted.takePriceUsd).isEqualTo(dto.takePriceUsd)
        assertThat(converted.priceHistory[0].date).isEqualTo(dto.priceHistory!![0].date)
        assertThat(converted.priceHistory[0].makeValue).isEqualTo(dto.priceHistory!![0].makeValue)
        assertThat(converted.priceHistory[0].takeValue).isEqualTo(dto.priceHistory!![0].takeValue)
    }

}