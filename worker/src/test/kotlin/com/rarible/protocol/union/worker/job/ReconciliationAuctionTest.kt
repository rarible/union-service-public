package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.service.EnrichmentAuctionEventService
import com.rarible.protocol.union.worker.config.ReconciliationProperties
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReconciliationAuctionTest {

    private val testPageSize = 50

    private val auctionService: AuctionService = mockk()
    private val auctionServiceRouter: BlockchainRouter<AuctionService> = mockk()
    private val auctionEventService: EnrichmentAuctionEventService = mockk()

    private val auctionReconciliationService = ReconciliationAuctionsJob(
        auctionServiceRouter,
        auctionEventService,
        mockk<WorkerProperties>() {
            every { reconciliation } returns ReconciliationProperties()
        }
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(auctionServiceRouter, auctionEventService)
        coEvery { auctionServiceRouter.getService(BlockchainDto.ETHEREUM) } returns auctionService
        coEvery { auctionEventService.onAuctionUpdated(any()) } returns Unit
    }

    @Test
    fun `run reconciliation task`() = runBlocking {
        mockGetAuctionsAll(null, testPageSize, mockPagination("1_1", testPageSize))
        mockGetAuctionsAll("1_1", testPageSize, mockPagination("1_2", testPageSize))
        mockGetAuctionsAll("1_2", testPageSize, mockPagination(null, 10))

        val result = auctionReconciliationService.handle(null, BlockchainDto.ETHEREUM.name).toList()

        assertEquals(2, result.size)
        assertEquals("1_1", result[0])
        assertEquals("1_2", result[1])
    }

    @Test
    fun `reconcile auctions - first page`() = runBlocking {
        val nextContinuation = "1_1"
        mockGetAuctionsAll(null, testPageSize, mockPagination(nextContinuation, testPageSize))

        val result = auctionReconciliationService.handleBatch(null, BlockchainDto.ETHEREUM)

        assertEquals(nextContinuation, result)
        coVerify(exactly = testPageSize) { auctionEventService.onAuctionUpdated(any()) }
    }

    @Test
    fun `reconcile auctions - next page`() = runBlocking {
        val lastContinuation = "1_1"
        val nextContinuation = "2_2"
        mockGetAuctionsAll(lastContinuation, testPageSize, mockPagination(nextContinuation, testPageSize))

        val result = auctionReconciliationService.handleBatch(lastContinuation, BlockchainDto.ETHEREUM)

        assertEquals(nextContinuation, result)
        coVerify(exactly = testPageSize) { auctionEventService.onAuctionUpdated(any()) }
    }

    @Test
    fun `reconcile auctions - last page`() = runBlocking {
        val lastContinuation = "1_1"
        val nextContinuation = null
        mockGetAuctionsAll(lastContinuation, testPageSize, mockPagination(nextContinuation, 50))

        val result = auctionReconciliationService.handleBatch(lastContinuation, BlockchainDto.ETHEREUM)

        assertNull(result)
        coVerify(exactly = 50) { auctionEventService.onAuctionUpdated(any()) }
    }

    @Test
    fun `reconcile auctions - empty page`() = runBlocking {
        mockGetAuctionsAll(null, testPageSize, mockPagination("1_1", 0))

        val result = auctionReconciliationService.handleBatch(null, BlockchainDto.ETHEREUM)

        assertNull(result)
        coVerify(exactly = 0) { auctionEventService.onAuctionUpdated(any()) }
    }

    private fun mockGetAuctionsAll(continuation: String?, size: Int, result: Slice<AuctionDto>) {
        coEvery {
            auctionService.getAuctionsAll(null, null, null, null, continuation, size)
        } returns result
    }

    private fun mockPagination(continuation: String?, count: Int): Slice<AuctionDto> {
        val auctions = ArrayList<AuctionDto>()
        for (i in 1..count) {
            auctions.add(mockk())
        }
        return Slice(continuation, auctions)
    }
}
