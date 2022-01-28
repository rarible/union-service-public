package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.enrichment.model.ReconciliationItemMarkEvent
import com.rarible.protocol.union.enrichment.model.ReconciliationOwnershipMarkEvent
import com.rarible.protocol.union.enrichment.repository.ItemReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipReconciliationMarkRepository
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FlowPreview
@IntegrationTest
class ReconciliationMarkEventHandlerIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var reconciliationMarkEventHandler: ReconciliationMarkEventHandler

    @Autowired
    lateinit var itemReconciliationMarkRepository: ItemReconciliationMarkRepository

    @Autowired
    lateinit var ownershipReconciliationMarkRepository: OwnershipReconciliationMarkRepository

    @Test
    fun `item marks are not duplicated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        assertThat(itemReconciliationMarkRepository.findAll(100)).hasSize(0)

        reconciliationMarkEventHandler.handle(ReconciliationItemMarkEvent(itemId))
        val allMarks = itemReconciliationMarkRepository.findAll(100)
        assertThat(allMarks).hasSize(1)
        assertThat(allMarks[0].retries).isEqualTo(0)

        // replacing retries
        itemReconciliationMarkRepository.save(allMarks[0].copy(retries = 5))

        reconciliationMarkEventHandler.handle(ReconciliationItemMarkEvent(itemId))
        val allMarksAfterUpdate = itemReconciliationMarkRepository.findAll(100)

        // Ensure retries is still 5
        assertThat(allMarksAfterUpdate).hasSize(1)
        assertThat(allMarksAfterUpdate[0].retries).isEqualTo(5)
    }

    @Test
    fun `ownership marks are not duplicated`() = runBlocking<Unit> {
        val ownershipId = randomEthOwnershipId()
        assertThat(ownershipReconciliationMarkRepository.findAll(100)).hasSize(0)

        reconciliationMarkEventHandler.handle(ReconciliationOwnershipMarkEvent(ownershipId))
        val allMarks = ownershipReconciliationMarkRepository.findAll(100)
        assertThat(allMarks).hasSize(1)
        assertThat(allMarks[0].retries).isEqualTo(0)

        // replacing retries
        ownershipReconciliationMarkRepository.save(allMarks[0].copy(retries = 5))

        reconciliationMarkEventHandler.handle(ReconciliationOwnershipMarkEvent(ownershipId))
        val allMarksAfterUpdate = ownershipReconciliationMarkRepository.findAll(100)

        // Ensure retries is still 5
        assertThat(allMarksAfterUpdate).hasSize(1)
        assertThat(allMarksAfterUpdate[0].retries).isEqualTo(5)
    }

}