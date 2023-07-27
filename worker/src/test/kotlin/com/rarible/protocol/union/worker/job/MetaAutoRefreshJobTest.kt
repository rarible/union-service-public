package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaRefreshService
import com.rarible.protocol.union.enrichment.model.MetaAutoRefreshState
import com.rarible.protocol.union.enrichment.model.MetaAutoRefreshStatus
import com.rarible.protocol.union.enrichment.repository.MetaAutoRefreshStateRepository
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
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
    private lateinit var collectionMetaRefreshService: CollectionMetaRefreshService

    @MockK
    private lateinit var metaRefreshRequestRepository: MetaRefreshRequestRepository

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
        val state1 = MetaAutoRefreshState(
            id = randomEthCollectionId().fullId(),
        )
        val state2 = MetaAutoRefreshState(
            id = randomEthCollectionId().fullId(),
        )
        coEvery {
            metaAutoRefreshStateRepository.loadToCheckCreated(match {
                it.isAfter(Instant.now().minus(Duration.ofDays(31))) && it.isBefore(
                    Instant.now().minus(Duration.ofDays(29))
                )
            })
        } returns flowOf(state1, state2)
        val state3 = MetaAutoRefreshState(
            id = randomEthCollectionId().fullId(),
            status = MetaAutoRefreshStatus.REFRESHED,
            lastRefreshedAt = Instant.now().minus(Duration.ofHours(2)),
        )
        val state4 = MetaAutoRefreshState(
            id = randomEthCollectionId().fullId(),
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

        coEvery { collectionMetaRefreshService.shouldAutoRefresh(state1.id) } returns false
        coEvery { collectionMetaRefreshService.shouldAutoRefresh(state2.id) } returns true
        coEvery { collectionMetaRefreshService.shouldAutoRefresh(state3.id) } returns false
        coEvery { collectionMetaRefreshService.shouldAutoRefresh(state4.id) } returns true

        coEvery { metaRefreshRequestRepository.save(any()) } returns Unit
        coEvery { metaAutoRefreshStateRepository.save(any()) } returns Unit

        metaAutoRefreshJob.handle()

        coVerify(exactly = 2) {
            metaRefreshRequestRepository.save(match {
                (it.collectionId == state2.id || it.collectionId == state4.id) && it.withSimpleHash
            })
            metaAutoRefreshStateRepository.save(match {
                it.status == MetaAutoRefreshStatus.REFRESHED && it.lastRefreshedAt != null &&
                    (it.id == state2.id || it.id == state4.id)
            })
        }
    }
}
