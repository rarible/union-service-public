package com.rarible.protocol.union.enrichment.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("enrichment_collection_statistics")
data class CollectionStatistics(
    @Id
    val id: ShortCollectionId,
    val itemCount: Long,
    val ownerCount: Long,
    val volumes: List<StatisticsPeriod>,
    val totalVolume: StatisticsValue,
    val floorPrice: StatisticsValue,
    val highestSale: StatisticsValue
)
