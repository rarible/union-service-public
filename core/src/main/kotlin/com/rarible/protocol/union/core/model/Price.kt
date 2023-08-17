package com.rarible.protocol.union.core.model

import java.math.BigDecimal

data class Price(
    val currency: String,
    val value: BigDecimal,
)
