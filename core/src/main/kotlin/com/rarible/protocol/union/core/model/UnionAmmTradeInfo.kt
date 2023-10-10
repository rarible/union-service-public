package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.OrderIdDto
import java.math.BigDecimal
import java.math.BigInteger

sealed class UnionAmmTradeInfo {
    abstract val orderId: OrderIdDto
}

// ------------------ Sudo Swap -------------------- //

data class UnionSudoSwapTradeInfo(
    override val orderId: OrderIdDto,
    val prices: List<UnionSudoSwapPriceInfoDto>
) : UnionAmmTradeInfo()

data class UnionSudoSwapPriceInfoDto(
    val price: BigInteger,
    val priceValue: BigDecimal,
    val priceUsd: BigDecimal? = null
)
