package com.rarible.protocol.union.enrichment.model

data class CollectionStatistics(
    val itemCount: Long,
    val ownerCount: Long,
    val volumes: List<StatisticsPeriod>,
    val totalVolume: StatisticsValue,
    val floorPrice: StatisticsValue?,
    val highestSale: StatisticsValue?
)
