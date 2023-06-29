package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityBurn
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivitySale
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
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

class ReconciliationLastSaleJobTest {

    private val testPageSize = 100

    private val activityService: ActivityService = mockk()
    private val orderServiceRouter: BlockchainRouter<ActivityService> = mockk()
    private val itemEventService: EnrichmentItemEventService = mockk()
    private val itemService: EnrichmentItemService = mockk()

    private val job = ReconciliationLastSaleJob(
        orderServiceRouter,
        itemEventService,
        itemService,
        mockk<WorkerProperties>() {
            every { reconciliation } returns ReconciliationProperties()
        }
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(orderServiceRouter, itemEventService, itemService)
        coEvery { orderServiceRouter.getService(BlockchainDto.ETHEREUM) } returns activityService
        coEvery { itemEventService.onActivity(any(), any(), any(), any()) } returns Unit
    }

    @Test
    fun `reconcile last sale`() = runBlocking<Unit> {
        val lastContinuation = "1_1"
        val nextContinuation = "2_2"

        val activities = (1..testPageSize).map { randomUnionActivitySale(randomEthItemId()) }

        mockGetActivities(lastContinuation, testPageSize, Slice(nextContinuation, activities))
        coEvery { itemService.fetchOrNull(any()) } answers {
            randomUnionItem((it.invocation.args[0] as ShortItemId).toDto())
        }

        val result = job.handleBatch(lastContinuation, BlockchainDto.ETHEREUM)

        assertThat(result).isEqualTo(nextContinuation)
        coVerify(exactly = testPageSize) { itemEventService.onActivity(any(), any(), any()) }
    }

    @Test
    fun `skip deleted items`() = runBlocking<Unit> {
        val notSaleItemId = randomEthItemId()
        val notSaleActivity = randomUnionActivityBurn(notSaleItemId)

        val burnedItemId = randomEthItemId()
        val burnedItemActivity = randomUnionActivitySale(burnedItemId)

        mockGetActivities(null, testPageSize, Slice(null, listOf(notSaleActivity, burnedItemActivity)))
        coEvery { itemService.fetchOrNull(ShortItemId(burnedItemId)) } returns null

        val result = job.handleBatch(null, BlockchainDto.ETHEREUM)

        assertThat(result).isNull()
        coVerify(exactly = 0) { itemEventService.onActivity(any(), any(), any()) }
    }

    private fun mockGetActivities(continuation: String?, size: Int, result: Slice<UnionActivity>): Unit {
        coEvery {
            activityService.getAllActivities(
                types = listOf(ActivityTypeDto.SELL),
                continuation = continuation,
                size = size,
                sort = ActivitySortDto.LATEST_FIRST
            )
        } returns result
    }
}
