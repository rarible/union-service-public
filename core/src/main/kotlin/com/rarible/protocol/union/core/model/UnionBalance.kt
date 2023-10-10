package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.UnionAddress
import java.math.BigDecimal
import java.math.BigInteger

data class UnionBalance(
    val currencyId: CurrencyIdDto,
    val owner: UnionAddress,
    val balance: BigInteger,
    val decimal: BigDecimal
)
