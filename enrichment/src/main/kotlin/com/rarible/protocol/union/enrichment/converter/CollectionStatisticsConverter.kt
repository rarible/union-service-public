package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.CollectionStatisticsDto
import com.rarible.protocol.union.dto.StatisticsPeriodDto
import com.rarible.protocol.union.dto.StatisticsValueDto
import com.rarible.protocol.union.enrichment.model.CollectionStatistics

object CollectionStatisticsConverter {

    fun convert(collectionStatistics: CollectionStatistics): CollectionStatisticsDto {
        return with(collectionStatistics) {
            CollectionStatisticsDto(
                itemCount = itemCount.toLong(),
                ownerCount = itemCount.toLong(),
                itemCountTotal = itemCount,
                ownerCountTotal = ownerCount,
                volumes = volumes.map {
                    StatisticsPeriodDto(
                        period = it.period,
                        value = with(it.value) {
                            StatisticsValueDto(
                                value = value,
                                valueUsd = valueUsd
                            )
                        },
                        changePercent = it.changePercent
                    )
                },
                totalVolume = with(totalVolume) {
                    StatisticsValueDto(
                        value = value,
                        valueUsd = valueUsd
                    )
                },
                floorPrice = floorPrice?.let {
                    StatisticsValueDto(
                        value = it.value,
                        valueUsd = it.valueUsd
                    )
                },
                highestSale = highestSale?.let {
                    StatisticsValueDto(
                        value = it.value,
                        valueUsd = it.valueUsd
                    )
                }
            )
        }
    }
}
