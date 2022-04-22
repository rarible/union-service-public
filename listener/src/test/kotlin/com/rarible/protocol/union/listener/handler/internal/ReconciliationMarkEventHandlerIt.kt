package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.model.ReconciliationMarkEvent
import com.rarible.protocol.union.core.model.ReconciliationMarkType
import com.rarible.protocol.union.enrichment.model.ReconciliationMark
import com.rarible.protocol.union.enrichment.repository.ReconciliationMarkRepository
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class ReconciliationMarkEventHandlerIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var reconciliationMarkEventHandler: ReconciliationMarkEventHandler

    @Autowired
    lateinit var reconciliationMarkRepository: ReconciliationMarkRepository

    @Test
    fun `marks are not duplicated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId()
        assertThat(findAllMarks(ReconciliationMarkType.ITEM)).hasSize(0)
        assertThat(findAllMarks(ReconciliationMarkType.OWNERSHIP)).hasSize(0)

        // Checking both legacy and actual formats works
        val itemEvent1 = ReconciliationMarkEvent(itemId.fullId(), ReconciliationMarkType.ITEM)
        val ownershipEvent1 = ReconciliationMarkEvent(ownershipId.fullId(), ReconciliationMarkType.OWNERSHIP)
        reconciliationMarkEventHandler.handle(itemEvent1)
        reconciliationMarkEventHandler.handle(ownershipEvent1)

        assertThat(findAllMarks(ReconciliationMarkType.ITEM)).hasSize(1)
        assertThat(findAllMarks(ReconciliationMarkType.OWNERSHIP)).hasSize(1)

        // Send same marks again
        val itemEvent2 = ReconciliationMarkEvent(itemId.fullId(), ReconciliationMarkType.ITEM)
        val ownershipEvent2 = ReconciliationMarkEvent(ownershipId.fullId(), ReconciliationMarkType.OWNERSHIP)
        reconciliationMarkEventHandler.handle(itemEvent2)
        reconciliationMarkEventHandler.handle(ownershipEvent2)

        // Checking there is no duplicates
        assertThat(findAllMarks(ReconciliationMarkType.ITEM)).hasSize(1)
        assertThat(findAllMarks(ReconciliationMarkType.OWNERSHIP)).hasSize(1)
    }

    @Test
    fun `retries are not reset`() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        val itemEvent = ReconciliationMarkEvent(itemId.fullId(), ReconciliationMarkType.ITEM)
        reconciliationMarkEventHandler.handle(itemEvent)

        val currentMark = findAllMarks(ReconciliationMarkType.ITEM)[0]
        reconciliationMarkRepository.save(currentMark.copy(retries = 5))

        // Send event again and ensure retry counter has not been changed
        reconciliationMarkEventHandler.handle(itemEvent)

        val lastMark = findAllMarks(ReconciliationMarkType.ITEM)[0]
        assertThat(lastMark.retries).isEqualTo(5)
    }

    private suspend fun findAllMarks(type: ReconciliationMarkType): List<ReconciliationMark> {
        return reconciliationMarkRepository.findByType(
            type,
            100
        )
    }

}
