package com.rarible.protocol.union.listener.clickhouse.repository

import com.rarible.protocol.union.dto.StatisticsPeriodDto
import com.rarible.protocol.union.listener.clickhouse.client.ClickHouseSimpleClient
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

@ClickHouseTest
@IntegrationTest
internal class ClickHouseCollectionStatisticsRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var clickHouseSimpleClient: ClickHouseSimpleClient

    @Autowired
    private lateinit var clickHouseCollectionStatisticsRepository: ClickHouseCollectionStatisticsRepository

    @Test
    fun `get statistics`() = runBlocking<Unit> {
        createTableAndFill()

        var fromIdExcluded: String? = null
        var resultMap = clickHouseCollectionStatisticsRepository.getAllStatistics(fromIdExcluded, LIMIT)
        assertThat(resultMap).hasSize(LIMIT)

        fromIdExcluded = resultMap.keys.last().toString()
        resultMap = clickHouseCollectionStatisticsRepository.getAllStatistics(fromIdExcluded, LIMIT)
        assertThat(resultMap).hasSize(3)

        val lastKey = resultMap.keys.last()
        assertThat(lastKey.toString()).isEqualTo(COLLECTION_ID)

        val lastValue = resultMap.values.last()
        assertThat(lastValue.itemCount).isEqualTo(51L)
        assertThat(lastValue.ownerCount).isEqualTo(100500L)
        assertThat(lastValue.volumes.map { it.period })
            .containsExactlyInAnyOrder(
                StatisticsPeriodDto.Period.DAY,
                StatisticsPeriodDto.Period.WEEK,
                StatisticsPeriodDto.Period.MONTH
            )
        assertThat(lastValue.volumes.single { it.period == StatisticsPeriodDto.Period.DAY }.changePercent)
            .isEqualTo(BigDecimal.ZERO)
        assertThat(lastValue.volumes.single { it.period == StatisticsPeriodDto.Period.MONTH }.changePercent)
            .isEqualTo("-88.5610746990782".toBigDecimal())
        assertThat(lastValue.totalVolume.value).isEqualTo("0.0010513063422169962".toBigDecimal())
        assertThat(lastValue.highestSale!!.valueUsd).isEqualTo("1.0288515821396607".toBigDecimal())

        val result = clickHouseCollectionStatisticsRepository.getCollectionStatistics(COLLECTION_ID)
        assertThat(result).isEqualTo(lastValue)
    }

    private fun createTableAndFill() = runBlocking<Unit> {
        val createTableQuery = readResourceAsString("/clickhouse/olap_create_table.sql")
        clickHouseSimpleClient.execute(createTableQuery)

        val inserts = readResourceAsString("/clickhouse/olap_fill_table.sql").split(";").filter { it.isNotBlank() }
        inserts.forEach { clickHouseSimpleClient.execute(it) }
    }

    private fun readResourceAsString(path: String): String = this::class.java.getResource(path)!!.readText()

    companion object {
        private const val COLLECTION_ID = "ETHEREUM:0x472cc4402f4819354da3334e66b02072ae2cd3bh"
        private const val LIMIT = 5
    }
}
