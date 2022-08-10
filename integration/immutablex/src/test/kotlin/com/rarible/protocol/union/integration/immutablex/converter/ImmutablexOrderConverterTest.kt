package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ImmutablexOrderDataV1Dto
import com.rarible.protocol.union.integration.data.randomEmxOrderBuySide
import com.rarible.protocol.union.integration.data.randomEmxOrderSellSide
import com.rarible.protocol.union.integration.data.randomImxOrder
import com.rarible.protocol.union.integration.data.randomImxOrderFee
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class ImmutablexOrderConverterTest {

    @Test
    fun `evaluate fees - sell order`() {
        val imxOrder = randomImxOrder().copy(
            buy = randomEmxOrderBuySide(quantity = BigInteger("100000"), quantityWithFees = BigInteger("105000")),
            fees = listOf(randomImxOrderFee(type = "royalty", amount = BigDecimal("5000")))
        )

        val converted = ImmutablexOrderConverter.convert(imxOrder, BlockchainDto.IMMUTABLEX)

        val data = converted.data as ImmutablexOrderDataV1Dto

        assertThat(data.payouts[0].value).isEqualTo(500)
    }

    @Test
    fun `evaluate fees - buy order`() {
        val imxOrder = randomImxOrder().copy(
            sell = randomEmxOrderBuySide(quantity = BigInteger("100000"), quantityWithFees = BigInteger("105000")),
            buy = randomEmxOrderSellSide(),
            fees = listOf(randomImxOrderFee(type = "royalty", amount = BigDecimal("5000")))
        )

        val converted = ImmutablexOrderConverter.convert(imxOrder, BlockchainDto.IMMUTABLEX)

        val data = converted.data as ImmutablexOrderDataV1Dto

        assertThat(data.payouts[0].value).isEqualTo(500)
    }
}