package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.OwnershipDto
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

    private val item: ItemDto = mockk()
    private val ownership: OwnershipDto = mockk()

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

        coEvery { refreshService.reconcileItem(any(), any()) } returns item

        job.reconcileMarkedRecords()

        coVerify(exactly = itemMarks.size) { refreshService.reconcileItem(any(), false) }
        assertThat(itemReconciliationMarkRepository.findAll(100)).hasSize(0)
    }

    @Test
    fun `reconcile ownerships`() = runBlocking<Unit> {
        val ownershipMarks = (1..50).map { randomOwnershipMark() }

        ownershipMarks.forEach { ownershipReconciliationMarkRepository.save(it) }
        assertThat(ownershipReconciliationMarkRepository.findAll(100)).hasSize(ownershipMarks.size)

        coEvery { refreshService.reconcileOwnership(any()) } returns ownership

        job.reconcileMarkedRecords()

        coVerify(exactly = ownershipMarks.size) { refreshService.reconcileOwnership(any()) }
        assertThat(ownershipReconciliationMarkRepository.findAll(100)).hasSize(0)
    }

    @Test
    fun `reconcile items - with fail`() = runBlocking<Unit> {
        val itemMarks = (1..20).map { randomItemMark() }

        itemMarks.forEach { itemReconciliationMarkRepository.save(it) }

        coEvery { refreshService.reconcileItem(any(), any()) } returns item
        coEvery { refreshService.reconcileItem(itemMarks[10].id.toDto(), false) } throws RuntimeException()

        job.reconcileMarkedRecords()

        coVerify(exactly = itemMarks.size) { refreshService.reconcileItem(any(), false) }
        // One failed item mark should remains in DB
        assertThat(itemReconciliationMarkRepository.findAll(100)).hasSize(1)
    }

    @Test
    fun `reconcile ownerships - with fail`() = runBlocking<Unit> {
        val itemMarks = (1..20).map { randomOwnershipMark() }

        itemMarks.forEach { ownershipReconciliationMarkRepository.save(it) }

        coEvery { refreshService.reconcileOwnership(any()) } returns ownership
        coEvery { refreshService.reconcileOwnership(itemMarks[10].id.toDto()) } throws RuntimeException()
        coEvery { refreshService.reconcileOwnership(itemMarks[11].id.toDto()) } throws RuntimeException()

        job.reconcileMarkedRecords()

        coVerify(exactly = itemMarks.size) { refreshService.reconcileOwnership(any()) }
        // Two failed ownership marks should remain in DB
        assertThat(ownershipReconciliationMarkRepository.findAll(100)).hasSize(2)
    }

}