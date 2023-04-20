package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityBurn
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivitySale
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityTransfer
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnership
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.worker.config.ReconciliationProperties
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReconciliationOwnershipSourceJobTest {

    private val testPageSize = 100

    private val activityService: ActivityService = mockk()
    private val orderServiceRouter: BlockchainRouter<ActivityService> = mockk()
    private val ownershipEventService: EnrichmentOwnershipEventService = mockk()
    private val ownershipService: EnrichmentOwnershipService = mockk()

    private val job = ReconciliationOwnershipSourceJob(
        orderServiceRouter,
        ownershipEventService,
        ownershipService,
        mockk<WorkerProperties>() {
            every { reconciliation } returns ReconciliationProperties()
        }
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(orderServiceRouter, ownershipEventService, ownershipService)
        coEvery { orderServiceRouter.getService(BlockchainDto.ETHEREUM) } returns activityService
        coEvery { ownershipEventService.onActivity(any(), any(), any()) } returns Unit
        coEvery { ownershipEventService.onActivityLegacy(any(), any(), any()) } returns Unit
    }

    @Test
    fun `reconcile source`() = runBlocking<Unit> {
        val lastContinuation = "1_1"
        val nextContinuation = "2_2"

        val activities = (1..testPageSize).map { randomUnionActivityTransfer(randomEthItemId()) }

        mockGetActivities(lastContinuation, testPageSize, Slice(nextContinuation, activities))
        coEvery { ownershipService.fetchOrNull(any()) } answers {
            randomUnionOwnership((it.invocation.args[0] as ShortOwnershipId).toDto())
        }

        val result = job.reconcileBatch(lastContinuation, BlockchainDto.ETHEREUM)

        assertThat(result).isEqualTo(nextContinuation)
        coVerify(exactly = testPageSize) { ownershipEventService.onActivity(any(), any(), any()) }
    }

    @Test
    fun `skip deleted ownerships`() = runBlocking<Unit> {
        val notSaleOwnershipId = randomEthOwnershipId()
        val notSaleActivity = randomUnionActivityBurn(notSaleOwnershipId.getItemId())

        val burnedOwnershipId = randomEthOwnershipId()
        val burnedOwnershipActivity = randomUnionActivitySale(burnedOwnershipId.getItemId())

        mockGetActivities(null, testPageSize, Slice(null, listOf(notSaleActivity, burnedOwnershipActivity)))
        coEvery { ownershipService.fetchOrNull(ShortOwnershipId(burnedOwnershipId)) } returns null

        val result = job.reconcileBatch(null, BlockchainDto.ETHEREUM)

        assertThat(result).isNull()
        coVerify(exactly = 0) { ownershipEventService.onActivityLegacy(any(), any(), any()) }
    }

    private fun mockGetActivities(continuation: String?, size: Int, result: Slice<UnionActivity>): Unit {
        coEvery {
            activityService.getAllActivities(
                types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.TRANSFER),
                continuation = continuation,
                size = size,
                sort = ActivitySortDto.LATEST_FIRST
            )
        } returns result
    }
}
