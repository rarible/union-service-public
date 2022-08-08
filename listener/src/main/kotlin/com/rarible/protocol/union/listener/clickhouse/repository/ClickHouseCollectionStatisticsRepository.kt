package com.rarible.protocol.union.listener.clickhouse.repository

import com.clickhouse.client.ClickHouseRecord
import com.clickhouse.client.ClickHouseValue
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.dto.StatisticsPeriodDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.CollectionStatistics
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.model.StatisticsPeriod
import com.rarible.protocol.union.enrichment.model.StatisticsValue
import com.rarible.protocol.union.listener.clickhouse.client.ClickHouseSimpleClient
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
@CaptureSpan(type = SpanType.DB)
class ClickHouseCollectionStatisticsRepository(
    private val clickHouseSimpleClient: ClickHouseSimpleClient
) {

    suspend fun getCollectionStatistics(collectionId: String): CollectionStatistics? {
        return try {
            val query = """
                SELECT
                    $ID_AND_STATISTICS_FIELDS
                FROM
                    $ALL_STATS_TABLE_NAME
                WHERE
                    collectionId = :collectionId
            """.trimIndent()

            clickHouseSimpleClient.queryForObject(
                query = query,
                arguments = mapOf("collectionId" to collectionId),
                recordToObject = { it.toCollectionStatistics() }
            )
        } catch (e: Exception) {
            logger.warn("Can't get collection statistics for collectionId={}", collectionId)
            throw e
        }
    }

    suspend fun getAllStatistics(fromIdExcluded: String?, limit: Int): Map<ShortCollectionId, CollectionStatistics> {
        return try {
            val query = """
                SELECT
                    $ID_AND_STATISTICS_FIELDS
                FROM
                    $ALL_STATS_TABLE_NAME
                WHERE
                    :fromIdExcluded IS NULL OR collectionId > :fromIdExcluded
                ORDER BY collectionId
                LIMIT :limit
            """.trimIndent()

            clickHouseSimpleClient.queryForList(
                query = query,
                arguments = mapOf(
                    "fromIdExcluded" to fromIdExcluded,
                    "limit" to limit
                ),
                recordToObject = { it.toCollectionId() to it.toCollectionStatistics() }
            ).toMap()
        } catch (e: Exception) {
            logger.warn("Can't get all statistics for fromIdExcluded={} and limit={}", fromIdExcluded, limit)
            throw e
        }
    }

    private fun ClickHouseRecord.toCollectionStatistics(): CollectionStatistics {
        val floorPriceNative = getBigDecimalOrNull(STATISTICS_FLOOR_PRICE_VALUE)
        val floorPriceUsd = getBigDecimalOrNull(STATISTICS_FLOOR_PRICE_VALUE_USD)

        return CollectionStatistics(
            itemCount = getLong(STATISTICS_ITEM_COUNT),
            ownerCount = getLong(STATISTICS_OWNER_COUNT),
            volumes = listOf(
                StatisticsPeriod(
                    period = StatisticsPeriodDto.Period.DAY,
                    value = StatisticsValue(
                        value = getBigDecimal(STATISTICS_VOLUMES_DAY_VALUE),
                        valueUsd = getBigDecimal(STATISTICS_VOLUMES_DAY_VALUE_USD)
                    ),
                    changePercent = getBigDecimalOrNull(STATISTICS_VOLUMES_DAY_PERCENTAGE) ?: BigDecimal.ZERO
                ),
                StatisticsPeriod(
                    period = StatisticsPeriodDto.Period.WEEK,
                    value = StatisticsValue(
                        value = getBigDecimal(STATISTICS_VOLUMES_WEEK_VALUE),
                        valueUsd = getBigDecimal(STATISTICS_VOLUMES_WEEK_VALUE_USD)
                    ),
                    changePercent = getBigDecimalOrNull(STATISTICS_VOLUMES_WEEK_PERCENTAGE) ?: BigDecimal.ZERO
                ),
                StatisticsPeriod(
                    period = StatisticsPeriodDto.Period.MONTH,
                    value = StatisticsValue(
                        value = getBigDecimal(STATISTICS_VOLUMES_MONTH_VALUE),
                        valueUsd = getBigDecimal(STATISTICS_VOLUMES_MONTH_VALUE_USD)
                    ),
                    changePercent = getBigDecimalOrNull(STATISTICS_VOLUMES_MONTH_PERCENTAGE) ?: BigDecimal.ZERO
                )
            ),
            totalVolume = StatisticsValue(
                value = getBigDecimal(STATISTICS_TOTAL_VOLUME_VALUE),
                valueUsd = getBigDecimal(STATISTICS_TOTAL_VOLUME_VALUE_USD)
            ),
            floorPrice = if (floorPriceNative != null && floorPriceUsd != null) {
                StatisticsValue(
                    value = floorPriceNative,
                    valueUsd = floorPriceUsd
                )
            } else {
                null
            },
            highestSale = StatisticsValue(
                value = getBigDecimal(STATISTICS_HIGHEST_SALE_VALUE),
                valueUsd = getBigDecimal(STATISTICS_HIGHEST_SALE_VALUE_USD)
            )
        )
    }

    private fun ClickHouseRecord.toCollectionId(): ShortCollectionId =
        ShortCollectionId(IdParser.parseCollectionId(getValue(COLLECTION_ID).asString()))

    private fun <T> ClickHouseRecord.getOrNull(name: String, converter: (ClickHouseValue) -> T): T? {
        val value = getValue(name)

        return if (value.isNullOrEmpty) null else converter(value)
    }

    private fun <T> ClickHouseRecord.get(name: String, converter: (ClickHouseValue) -> T): T {
        return getOrNull(name, converter)
            ?: throw IllegalStateException("Field with name=$name has null value")
    }

    private fun ClickHouseRecord.getBigDecimalOrNull(name: String): BigDecimal? {
        return getOrNull(name) { it.asDouble().toBigDecimal() }
    }

    private fun ClickHouseRecord.getBigDecimal(name: String): BigDecimal {
        return get(name) { it.asDouble().toBigDecimal() }
    }

    private fun ClickHouseRecord.getLong(name: String): Long {
        return get(name) { it.asLong() }
    }

    companion object {
        private val logger by Logger()
        private const val ALL_STATS_TABLE_NAME = "marketplace_all_stats"
        private const val COLLECTION_ID = "collectionId"
        private const val STATISTICS_ITEM_COUNT = "totalItemSupply"
        private const val STATISTICS_OWNER_COUNT = "ownersCount"
        private const val STATISTICS_VOLUMES_DAY_VALUE = "gmvNative_1d"
        private const val STATISTICS_VOLUMES_DAY_VALUE_USD = "gmvUsd_1d"
        private const val STATISTICS_VOLUMES_DAY_PERCENTAGE = "gmvUsd_1d_changePercentage"
        private const val STATISTICS_VOLUMES_WEEK_VALUE = "gmvNative_7d"
        private const val STATISTICS_VOLUMES_WEEK_VALUE_USD = "gmvUsd_7d"
        private const val STATISTICS_VOLUMES_WEEK_PERCENTAGE = "gmvUsd_7d_changePercentage"
        private const val STATISTICS_VOLUMES_MONTH_VALUE = "gmvNative_30d"
        private const val STATISTICS_VOLUMES_MONTH_VALUE_USD = "gmvUsd_30d"
        private const val STATISTICS_VOLUMES_MONTH_PERCENTAGE = "gmvUsd_30d_changePercentage"
        private const val STATISTICS_TOTAL_VOLUME_VALUE = "totalGmvNative"
        private const val STATISTICS_TOTAL_VOLUME_VALUE_USD = "totalGmvUsd"
        private const val STATISTICS_FLOOR_PRICE_VALUE = "floorPriceNative"
        private const val STATISTICS_FLOOR_PRICE_VALUE_USD = "floorPriceUsd"
        private const val STATISTICS_HIGHEST_SALE_VALUE = "highestSaleNative"
        private const val STATISTICS_HIGHEST_SALE_VALUE_USD = "highestSaleUsd"
        private val ID_AND_STATISTICS_FIELDS = """
            $COLLECTION_ID,
            $STATISTICS_ITEM_COUNT,
            $STATISTICS_OWNER_COUNT,
            $STATISTICS_VOLUMES_DAY_VALUE,
            $STATISTICS_VOLUMES_DAY_VALUE_USD,
            $STATISTICS_VOLUMES_DAY_PERCENTAGE,
            $STATISTICS_VOLUMES_WEEK_VALUE,
            $STATISTICS_VOLUMES_WEEK_VALUE_USD,
            $STATISTICS_VOLUMES_WEEK_PERCENTAGE,
            $STATISTICS_VOLUMES_MONTH_VALUE,
            $STATISTICS_VOLUMES_MONTH_VALUE_USD,
            $STATISTICS_VOLUMES_MONTH_PERCENTAGE,
            $STATISTICS_TOTAL_VOLUME_VALUE,
            $STATISTICS_TOTAL_VOLUME_VALUE_USD,
            $STATISTICS_FLOOR_PRICE_VALUE,
            $STATISTICS_FLOOR_PRICE_VALUE_USD,
            $STATISTICS_HIGHEST_SALE_VALUE,
            $STATISTICS_HIGHEST_SALE_VALUE_USD
        """.trimIndent()
    }
}
