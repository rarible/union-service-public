package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV2Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV3BuyDto
import com.rarible.protocol.dto.OrderRaribleV2DataV3SellDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV2Dto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV3BuyDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV3SellDto
import com.rarible.protocol.union.dto.PayoutDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import randomEthRaribleV2OrderFormDto
import randomUnionAddress

class UnionOrderFormConverterTest {

    @Test
    fun `rarible form - ok`() {
        val dto = randomEthRaribleV2OrderFormDto()

        val result = UnionOrderFormConverter.convert(dto)

        assertThat(result.maker.prefixed()).isEqualTo(dto.maker.value)
        assertThat(result.taker?.prefixed()).isEqualTo(dto.taker?.value)
        assertThat(result.start).isEqualTo(dto.startedAt?.epochSecond)
        assertThat(result.end).isEqualTo(dto.endedAt.epochSecond)
        assertThat(result.salt).isEqualTo(dto.salt)
        assertThat(result.signature.prefixed()).isEqualTo(dto.signature)
        assertThat(result.take.value).isEqualTo(dto.take.value)
        assertThat(result.make.value).isEqualTo(dto.make.value)
    }

    @Test
    fun `rarible v2 data v1 - ok`() {
        val payout = PayoutDto(randomUnionAddress(), randomInt())
        val fee = PayoutDto(randomUnionAddress(), randomInt())

        val dto = EthOrderDataRaribleV2DataV1Dto(
            payouts = listOf(payout),
            originFees = listOf(fee)
        )
        val result = UnionOrderFormConverter.convert(dto) as OrderRaribleV2DataV1Dto

        assertThat(result.payouts).hasSize(1)
        assertThat(result.payouts[0].account.prefixed()).isEqualTo(payout.account.value)
        assertThat(result.payouts[0].value).isEqualTo(payout.value)
        assertThat(result.originFees).hasSize(1)
        assertThat(result.originFees[0].account.prefixed()).isEqualTo(fee.account.value)
        assertThat(result.originFees[0].value).isEqualTo(fee.value)
    }

    @Test
    fun `rarible v2 data v2 - ok`() {
        val payout = PayoutDto(randomUnionAddress(), randomInt())
        val fee = PayoutDto(randomUnionAddress(), randomInt())

        val dto = EthOrderDataRaribleV2DataV2Dto(
            payouts = listOf(payout),
            originFees = listOf(fee),
            isMakeFill = true
        )
        val result = UnionOrderFormConverter.convert(dto) as OrderRaribleV2DataV2Dto

        assertThat(result.isMakeFill).isEqualTo(true)
        assertThat(result.payouts).hasSize(1)
        assertThat(result.payouts[0].account.prefixed()).isEqualTo(payout.account.value)
        assertThat(result.payouts[0].value).isEqualTo(payout.value)
        assertThat(result.originFees).hasSize(1)
        assertThat(result.originFees[0].account.prefixed()).isEqualTo(fee.account.value)
        assertThat(result.originFees[0].value).isEqualTo(fee.value)
    }

    @Test
    fun `rarible v2 data v3 sell - ok`() {
        val payout = PayoutDto(randomUnionAddress(), randomInt())
        val fee = PayoutDto(randomUnionAddress(), randomInt())

        val dto = EthOrderDataRaribleV2DataV3SellDto(
            payout = payout,
            originFeeFirst = fee,
            originFeeSecond = null,
            maxFeesBasePoint = randomInt(),
            marketplaceMarker = randomWord()
        )

        val result = UnionOrderFormConverter.convert(dto) as OrderRaribleV2DataV3SellDto

        assertThat(result.maxFeesBasePoint).isEqualTo(dto.maxFeesBasePoint)
        assertThat(result.marketplaceMarker!!.prefixed()).isEqualTo(dto.marketplaceMarker)
        assertThat(result.payout!!.account.prefixed()).isEqualTo(payout.account.value)
        assertThat(result.payout!!.value).isEqualTo(payout.value)
        assertThat(result.originFeeFirst!!.account.prefixed()).isEqualTo(fee.account.value)
        assertThat(result.originFeeFirst!!.value).isEqualTo(fee.value)
        assertThat(result.originFeeSecond).isNull()
    }

    @Test
    fun `rarible v2 data v3 buy - ok`() {
        val payout = PayoutDto(randomUnionAddress(), randomInt())
        val fee = PayoutDto(randomUnionAddress(), randomInt())

        val dto = EthOrderDataRaribleV2DataV3BuyDto(
            payout = payout,
            originFeeFirst = fee,
            originFeeSecond = null,
            marketplaceMarker = randomWord()
        )

        val result = UnionOrderFormConverter.convert(dto) as OrderRaribleV2DataV3BuyDto

        assertThat(result.marketplaceMarker!!.prefixed()).isEqualTo(dto.marketplaceMarker)
        assertThat(result.payout!!.account.prefixed()).isEqualTo(payout.account.value)
        assertThat(result.payout!!.value).isEqualTo(payout.value)
        assertThat(result.originFeeFirst!!.account.prefixed()).isEqualTo(fee.account.value)
        assertThat(result.originFeeFirst!!.value).isEqualTo(fee.value)
        assertThat(result.originFeeSecond).isNull()
    }
}
