package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.enrichment.repository.ItemReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.listener.test.data.randomItemMark
import com.rarible.protocol.union.listener.test.data.randomOwnershipMark
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FlowPreview
@IntegrationTest
class ReconciliationMarkJobIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var itemReconciliationMarkRepository: ItemReconciliationMarkRepository

    @Autowired
    lateinit var ownershipReconciliationMarkRepository: OwnershipReconciliationMarkRepository

    private val refreshService: EnrichmentRefreshService = mockk()

    lateinit var job: ReconciliationMarkJob

    private val itemEvent: ItemEventDto = mockk()
    private val ownershipEvent: OwnershipEventDto = mockk()

    @BeforeEach
    fun beforeEach() {
        job = ReconciliationMarkJob(
            itemReconciliationMarkRepository,
            ownershipReconciliationMarkRepository,
            refreshService
        )
        clearMocks(refreshService)
    }

    @Test
    fun `reconcile items`() = runBlocking<Unit> {
        val itemMarks = (1..50).map { randomItemMark() }

        itemMarks.forEach { itemReconciliationMarkRepository.save(it) }
        assertThat(itemReconciliationMarkRepository.findAll(100)).hasSize(itemMarks.size)

        coEvery { refreshService.reconcileItem(any(), any()) } returns itemEvent

        job.reconcileMarkedRecords()

        coVerify(exactly = itemMarks.size) { refreshService.reconcileItem(any(), false) }
        assertThat(itemReconciliationMarkRepository.findAll(100)).hasSize(0)
    }

    @Test
    fun `reconcile ownerships`() = runBlocking<Unit> {
        val ownershipMarks = (1..50).map { randomOwnershipMark() }

        ownershipMarks.forEach { ownershipReconciliationMarkRepository.save(it) }
        assertThat(ownershipReconciliationMarkRepository.findAll(100)).hasSize(ownershipMarks.size)

        coEvery { refreshService.reconcileOwnership(any()) } returns ownershipEvent

        job.reconcileMarkedRecords()

        coVerify(exactly = ownershipMarks.size) { refreshService.reconcileOwnership(any()) }
        assertThat(ownershipReconciliationMarkRepository.findAll(100)).hasSize(0)
    }

    @Test
    fun `reconcile items - with fail`() = runBlocking<Unit> {
        val itemMarks = (1..10).map { randomItemMark() }

        itemMarks.forEach { itemReconciliationMarkRepository.save(it) }

        coEvery { refreshService.reconcileItem(any(), any()) } returns itemEvent
        coEvery { refreshService.reconcileItem(itemMarks[5].id.toDto(), false) } throws RuntimeException()

        job.reconcileMarkedRecords()

        // 1 additional call for single retry for one corrupted item
        coVerify(exactly = itemMarks.size + 1) { refreshService.reconcileItem(any(), false) }
        // One failed item mark should remain in DB
        val failedMarks = itemReconciliationMarkRepository.findAll(100)
        assertThat(failedMarks).hasSize(1)
        assertThat(failedMarks[0].retries).isEqualTo(2)
    }

    @Test
    fun `reconcile ownerships - with fail`() = runBlocking<Unit> {
        val itemMarks = (1..30).map { randomOwnershipMark() }

        itemMarks.forEach { ownershipReconciliationMarkRepository.save(it) }

        coEvery { refreshService.reconcileOwnership(any()) } returns ownershipEvent
        coEvery { refreshService.reconcileOwnership(itemMarks[10].id.toDto()) } throws RuntimeException()
        coEvery { refreshService.reconcileOwnership(itemMarks[11].id.toDto()) } throws RuntimeException()

        job.reconcileMarkedRecords()

        // 4 additional calls (2 retries for each of 2 failed)
        coVerify(exactly = itemMarks.size + 4) { refreshService.reconcileOwnership(any()) }
        // Two failed ownership marks should remain in DB
        val failedMarks = ownershipReconciliationMarkRepository.findAll(100)
        assertThat(failedMarks).hasSize(2)
        assertThat(failedMarks[0].retries).isEqualTo(3)
        assertThat(failedMarks[1].retries).isEqualTo(3)
    }

}