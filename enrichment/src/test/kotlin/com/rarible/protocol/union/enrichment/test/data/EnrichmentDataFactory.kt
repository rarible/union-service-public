package com.rarible.protocol.union.enrichment.test.data

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomLong
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.StatisticsPeriodDto
import com.rarible.protocol.union.enrichment.converter.ShortCollectionConverter
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.model.CollectionStatistics
import com.rarible.protocol.union.enrichment.model.StatisticsPeriod
import com.rarible.protocol.union.enrichment.model.StatisticsValue
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId

fun randomShortItem() = ShortItemConverter.convert(randomUnionItem(randomEthItemId()))
fun randomShortItem(id: ItemIdDto) = ShortItemConverter.convert(randomUnionItem(id))

fun randomShortCollection(id: CollectionIdDto) = ShortCollectionConverter.convert(
    collection = randomUnionCollection(id),
    statistics = randomCollectionStatistics()
)

fun randomCollectionStatistics() = CollectionStatistics(
    itemCount = randomLong(),
    ownerCount = randomLong(),
    volumes = listOf(
        randomStatisticsPeriod(StatisticsPeriodDto.Period.DAY),
        randomStatisticsPeriod(StatisticsPeriodDto.Period.WEEK),
        randomStatisticsPeriod(StatisticsPeriodDto.Period.MONTH)
    ),
    totalVolume = randomStatisticsValue(),
    floorPrice = randomStatisticsValue(),
    highestSale = randomStatisticsValue()
)

fun randomStatisticsPeriod(period: StatisticsPeriodDto.Period) = StatisticsPeriod(
    period = period,
    value = randomStatisticsValue(),
    changePercent = randomBigDecimal()
)

fun randomStatisticsValue() = StatisticsValue(
    value = randomBigDecimal(),
    valueUsd = randomBigDecimal()
)

fun randomShortOwnership() = ShortOwnershipConverter.convert(randomUnionOwnership())
fun randomShortOwnership(id: ItemIdDto) = ShortOwnershipConverter.convert(randomUnionOwnership(id))
fun randomShortOwnership(id: OwnershipIdDto) = ShortOwnershipConverter.convert(randomUnionOwnership(id))
