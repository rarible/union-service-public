package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.enrichment.meta.item.ItemMetaRefreshService
import com.rarible.protocol.union.enrichment.model.MetaAutoRefreshState
import com.rarible.protocol.union.enrichment.model.MetaAutoRefreshStatus
import com.rarible.protocol.union.enrichment.repository.MetaAutoRefreshStateRepository
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.worker.config.MetaAutoRefreshProperties
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class MetaAutoRefreshJobTest {
    @InjectMockKs
    private lateinit var metaAutoRefreshJob: MetaAutoRefreshJob

    @MockK
    private lateinit var metaAutoRefreshStateRepository: MetaAutoRefreshStateRepository

    @MockK
    private lateinit var itemMetaRefreshService: ItemMetaRefreshService

    @MockK
    private lateinit var esActivityRepository: EsActivityRepository

    @SpyK
    private var simpleHashEnabled: Boolean = true

    @SpyK
    private var properties: WorkerProperties = WorkerProperties(
        metaAutoRefresh = MetaAutoRefreshProperties()
    )

    @SpyK
    private var meterRegistry: MeterRegistry = SimpleMeterRegistry()

    @Test
    fun handle() = runTest {
        coEvery {
            esActivityRepository.findTradedDistinctCollections(any(), any())
        } returns emptyList()

        val collectionId1 = randomEthCollectionId()
        val state1 = MetaAutoRefreshState(id = collectionId1.fullId())

        val collectionId2 = randomEthCollectionId()
        val state2 = MetaAutoRefreshState(id = collectionId2.fullId())

        coEvery {
            metaAutoRefreshStateRepository.loadToCheckCreated(match {
                it.isAfter(Instant.now().minus(Duration.ofDays(31))) && it.isBefore(
                    Instant.now().minus(Duration.ofDays(29))
                )
            })
        } returns flowOf(state1, state2)

        val collectionId3 = randomEthCollectionId()
        val state3 = MetaAutoRefreshState(
            id = collectionId3.fullId(),
            status = MetaAutoRefreshStatus.REFRESHED,
            lastRefreshedAt = Instant.now().minus(Duration.ofHours(2)),
        )

        val collectionId4 = randomEthCollectionId()
        val state4 = MetaAutoRefreshState(
            id = collectionId4.fullId(),
            status = MetaAutoRefreshStatus.REFRESHED,
            lastRefreshedAt = Instant.now().minus(Duration.ofHours(2)),
        )
        coEvery {
            metaAutoRefreshStateRepository.loadToCheckRefreshed(match {
                it.isAfter(Instant.now().minus(Duration.ofDays(31))) && it.isBefore(
                    Instant.now().minus(Duration.ofDays(29))
                )
            }, match {
                it.isAfter(Instant.now().minus(Duration.ofDays(15))) && it.isBefore(
                    Instant.now().minus(Duration.ofDays(13))
                )
            })
        } returns flowOf(state3, state4)

        coEvery { itemMetaRefreshService.runAutoRefreshIfAllowed(collectionId1, simpleHashEnabled) } returns false
        coEvery { itemMetaRefreshService.runAutoRefreshIfAllowed(collectionId2, simpleHashEnabled) } returns true
        coEvery { itemMetaRefreshService.runAutoRefreshIfAllowed(collectionId3, simpleHashEnabled) } returns false
        coEvery { itemMetaRefreshService.runAutoRefreshIfAllowed(collectionId4, simpleHashEnabled) } returns true

        coEvery { metaAutoRefreshStateRepository.save(any()) } returns Unit

        metaAutoRefreshJob.handle()

        coVerify(exactly = 2) {
            metaAutoRefreshStateRepository.save(match {
                it.status == MetaAutoRefreshStatus.REFRESHED && it.lastRefreshedAt != null &&
                    (it.id == state2.id || it.id == state4.id)
            })
        }
    }
}
