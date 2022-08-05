package com.rarible.protocol.union.enrichment.model

import java.math.BigDecimal

data class StatisticsValue(
    val value: BigDecimal,
    val valueUsd: BigDecimal
)
