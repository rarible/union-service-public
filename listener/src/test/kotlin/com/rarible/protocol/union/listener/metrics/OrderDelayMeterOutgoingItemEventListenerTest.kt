package com.rarible.protocol.union.listener.metrics

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.CompositeRegisteredTimer
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant

internal class OrderDelayMeterOutgoingItemEventListenerTest {
    private val clock = mockk<Clock>()
    private val orderCompositeRegisteredTimer = mockk<CompositeRegisteredTimer>()
    private val currencyService = mockk<CurrencyService> {
        coEvery { toUsd(any(), any(), any(), any()) } returns null
    }
    private val orderMeterListener = OrderDelayMeterOutgoingItemEventListener(clock, orderCompositeRegisteredTimer)
    private val ethOrderConverter = EthOrderConverter(currencyService)

    @Test
    fun `should meter ownership delay`() = runBlocking {
        val lastUpdatedAt = Instant.ofEpochMilli(1000)
        val now = Instant.ofEpochMilli(3000)

        val order = ethOrderConverter.convert(randomEthV2OrderDto(), BlockchainDto.ETHEREUM).copy(lastUpdatedAt = lastUpdatedAt)
        val orderEvent = OrderUpdateEventDto(order.id, randomString(), order)

        val expectedDelay = Duration.ofMillis(2000)
        val expectedBlockchain = BlockchainDto.ETHEREUM

        every { clock.instant() } returns now
        every { orderCompositeRegisteredTimer.record(expectedDelay, expectedBlockchain) } returns Unit

        orderMeterListener.onEvent(orderEvent)

        verify(exactly = 1) { orderCompositeRegisteredTimer.record(expectedDelay, expectedBlockchain) }
    }
}