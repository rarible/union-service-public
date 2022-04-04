package com.rarible.protocol.union.listener.metrics

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.CompositeRegisteredTimer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.enrichment.test.data.randomItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant

internal class ItemDelayMeterOutgoingItemEventListenerTest {
    private val clock = mockk<Clock>()
    private val itemCompositeRegisteredTimer = mockk<CompositeRegisteredTimer>()
    private val itemMeterListener = ItemDelayMeterOutgoingItemEventListener(clock, itemCompositeRegisteredTimer)

    @Test
    fun `should meter item delay`() = runBlocking {
        val lastUpdatedAt = Instant.ofEpochMilli(1000)
        val now = Instant.ofEpochMilli(3000)

        val itemId = randomEthItemId()
        val item = randomItemDto(itemId).copy(lastUpdatedAt = lastUpdatedAt)
        val itemEvent = ItemUpdateEventDto(itemId, randomString(), item)

        val expectedDelay = Duration.ofMillis(2000)
        val expectedBlockchain = BlockchainDto.ETHEREUM

        every { clock.instant() } returns now
        every { itemCompositeRegisteredTimer.record(expectedDelay, expectedBlockchain) } returns Unit

        itemMeterListener.onEvent(itemEvent)

        verify(exactly = 1) { itemCompositeRegisteredTimer.record(expectedDelay, expectedBlockchain) }
    }

    @Test
    fun `should no meter for item delete event`() = runBlocking {
        val itemId = randomEthItemId()
        val itemEvent = ItemDeleteEventDto(itemId, randomString())

        itemMeterListener.onEvent(itemEvent)

        verify(exactly = 0) { itemCompositeRegisteredTimer.record(any(), any()) }
    }
}

