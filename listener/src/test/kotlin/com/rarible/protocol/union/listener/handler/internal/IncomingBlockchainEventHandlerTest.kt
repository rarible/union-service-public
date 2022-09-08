package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.kafka.KafkaSendResult
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityMint
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomPolygonItemId
import com.rarible.protocol.union.listener.handler.UnionActivityEventHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class IncomingBlockchainEventHandlerTest {

    private val ethereumProducer: RaribleKafkaProducer<UnionInternalBlockchainEvent> = mockk()
    private val polygonProducer: RaribleKafkaProducer<UnionInternalBlockchainEvent> = mockk()
    private val producer = UnionInternalBlockchainEventProducer(
        mapOf(
            BlockchainDto.ETHEREUM to ethereumProducer,
            BlockchainDto.POLYGON to polygonProducer,
        )
    )

    private val handler = UnionActivityEventHandler(producer)

    @Test
    fun `single event`() = runBlocking<Unit> {
        val a1 = randomUnionActivityMint(randomEthItemId())
        val a2 = randomUnionActivityMint(randomPolygonItemId())

        val expectedEthereum = KafkaEventFactory.internalActivityEvent(a1)
        val expectedPolygon = KafkaEventFactory.internalActivityEvent(a2)

        coEvery { ethereumProducer.send(expectedEthereum) } returns KafkaSendResult.Success("")
        handler.onEvent(a1)
        coVerify(exactly = 1) { ethereumProducer.send(expectedEthereum) }

        coEvery { polygonProducer.send(expectedPolygon) } returns KafkaSendResult.Success("")
        handler.onEvent(a2)
        coVerify(exactly = 1) { polygonProducer.send(expectedPolygon) }
    }

    @Test
    fun `multiple events`() = runBlocking<Unit> {
        val a1 = randomUnionActivityMint(randomEthItemId())
        val a2 = randomUnionActivityMint(randomEthItemId())
        val a3 = randomUnionActivityMint(randomPolygonItemId())

        val expectedEthereum = listOf(
            KafkaEventFactory.internalActivityEvent(a1),
            KafkaEventFactory.internalActivityEvent(a2)
        )
        val expectedPolygon = listOf(KafkaEventFactory.internalActivityEvent(a3))

        coEvery { ethereumProducer.send(expectedEthereum) } returns emptyFlow()
        coEvery { polygonProducer.send(expectedPolygon) } returns emptyFlow()

        // Order should be kept
        handler.onEvents(listOf(a3, a1, a2))

        coVerify(exactly = 1) { ethereumProducer.send(expectedEthereum) }
        coVerify(exactly = 1) { polygonProducer.send(expectedPolygon) }
    }
}