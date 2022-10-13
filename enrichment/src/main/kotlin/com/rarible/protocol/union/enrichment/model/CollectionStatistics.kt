package com.rarible.protocol.union.enrichment.model

import java.math.BigInteger

data class CollectionStatistics(
    val itemCount: BigInteger,
    val ownerCount: BigInteger,
    val volumes: List<StatisticsPeriod>,
    val totalVolume: StatisticsValue,
    val floorPrice: StatisticsValue?,
    val highestSale: StatisticsValue?
)
