package com.rarible.protocol.union.listener.metrics

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.CompositeRegisteredTimer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.test.data.randomOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant

internal class OwnershipDelayMeterOutgoingItemEventListenerTest {
    private val clock = mockk<Clock>()
    private val ownershipCompositeRegisteredTimer = mockk<CompositeRegisteredTimer>()
    private val ownershipMeterListener = OwnershipDelayMeterOutgoingItemEventListener(clock, ownershipCompositeRegisteredTimer)

    @Test
    fun `should meter ownership delay`() = runBlocking {
        val lastUpdatedAt = Instant.ofEpochMilli(1000)
        val now = Instant.ofEpochMilli(3000)

        val ownershipId = randomEthOwnershipId()
        val ownership = randomOwnershipDto(ownershipId).copy(createdAt = lastUpdatedAt)
        val ownershipEvent = OwnershipUpdateEventDto(ownershipId, randomString(), ownership)

        val expectedDelay = Duration.ofMillis(2000)
        val expectedBlockchain = BlockchainDto.ETHEREUM

        every { clock.instant() } returns now
        every { ownershipCompositeRegisteredTimer.record(expectedDelay, expectedBlockchain) } returns Unit

        ownershipMeterListener.onEvent(ownershipEvent)

        verify(exactly = 1) { ownershipCompositeRegisteredTimer.record(expectedDelay, expectedBlockchain) }
    }

    @Test
    fun `should no meter for ownership delete event`() = runBlocking {
        val ownershipId = randomEthOwnershipId()
        val ownershipEvent = OwnershipDeleteEventDto(ownershipId, randomString())

        ownershipMeterListener.onEvent(ownershipEvent)

        verify(exactly = 0) { ownershipCompositeRegisteredTimer.record(any(), any()) }
    }
}

