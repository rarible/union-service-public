package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.StatisticsPeriodDto
import java.math.BigDecimal

data class StatisticsPeriod(
    val period: StatisticsPeriodDto.Period,
    val value: StatisticsValue,
    val changePercent: BigDecimal
)
