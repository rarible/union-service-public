package com.rarible.protocol.union.worker.task

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.test.data.randomCollectionStatistics
import com.rarible.protocol.union.enrichment.clickhouse.repository.ClickHouseCollectionStatisticsRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionEventService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class CollectionStatisticsResyncTaskTest {
    @InjectMockKs
    private lateinit var collectionStatisticsResyncTask: CollectionStatisticsResyncTask

    @MockK
    private lateinit var clickHouseCollectionStatisticsRepository: ClickHouseCollectionStatisticsRepository

    @MockK
    private lateinit var enrichmentCollectionEventService: EnrichmentCollectionEventService

    @Test
    fun resync() = runBlocking<Unit> {
        val collectionId1 = ShortCollectionId(
            blockchain = BlockchainDto.ETHEREUM,
            collectionId = "1"
        )
        val collectionId2 = ShortCollectionId(
            blockchain = BlockchainDto.ETHEREUM,
            collectionId = "2"
        )
        val collectionId3 = ShortCollectionId(
            blockchain = BlockchainDto.ETHEREUM,
            collectionId = "3"
        )
        val statistics1 = randomCollectionStatistics()
        val statistics2 = randomCollectionStatistics()
        val statistics3 = randomCollectionStatistics()
        coEvery { clickHouseCollectionStatisticsRepository.getAllStatistics("from", 2) } returns
            mapOf(
                collectionId1 to statistics1,
                collectionId2 to statistics2
            )

        coEvery { clickHouseCollectionStatisticsRepository.getAllStatistics("ETHEREUM:2", 2) } returns
            mapOf(collectionId3 to statistics3)

        coEvery { enrichmentCollectionEventService.onCollectionStatisticsUpdate(any(), any(), any()) } returns Unit

        val result = collectionStatisticsResyncTask.runLongTask("from", "2").toList()

        coVerify {
            enrichmentCollectionEventService.onCollectionStatisticsUpdate(
                collectionId = collectionId1, statistics = statistics1, notificationEnabled = true
            )
        }
        coVerify {
            enrichmentCollectionEventService.onCollectionStatisticsUpdate(
                collectionId = collectionId2, statistics = statistics2, notificationEnabled = true
            )
        }
        coVerify {
            enrichmentCollectionEventService.onCollectionStatisticsUpdate(
                collectionId = collectionId3, statistics = statistics3, notificationEnabled = true
            )
        }
        assertThat(result).containsExactly("ETHEREUM:2", "ETHEREUM:3")
    }
}