package com.rarible.protocol.union.core.producer

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.core.model.UnionInternalOrderEvent
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import randomOrderId
import randomUnionOrder

class UnionInternalEventProducerTest {

    private val ethereumProducer: RaribleKafkaProducer<UnionInternalBlockchainEvent> = mockk {
        coEvery { send(any<KafkaMessage<UnionInternalBlockchainEvent>>()) } returns KafkaSendResult.Success("")
        coEvery { send(any<List<KafkaMessage<UnionInternalBlockchainEvent>>>()) } returns emptyFlow()
    }
    private val polygonProducer: RaribleKafkaProducer<UnionInternalBlockchainEvent> = mockk {
        coEvery { send(any<KafkaMessage<UnionInternalBlockchainEvent>>()) } returns KafkaSendResult.Success("")
        coEvery { send(any<List<KafkaMessage<UnionInternalBlockchainEvent>>>()) } returns emptyFlow()
    }

    private val producer = UnionInternalBlockchainEventProducer(
        mapOf(
            BlockchainDto.ETHEREUM to ethereumProducer,
            BlockchainDto.POLYGON to polygonProducer,
        )
    )

    private val eventCountMetrics = mockk<EventCountMetrics> {
        coEvery { eventSent(any(), any(), any(), any()) } returns Unit
    }

    private val handler = UnionInternalOrderEventProducer(producer, eventCountMetrics)

    @Test
    fun `single event`() = runBlocking<Unit> {
        val a1 = UnionOrderUpdateEvent(randomUnionOrder(randomOrderId(BlockchainDto.ETHEREUM)), null)
        val a2 = UnionOrderUpdateEvent(randomUnionOrder(randomOrderId(BlockchainDto.POLYGON)), null)

        handler.send(a1)
        verifyEvent(a1.order, ethereumProducer)

        handler.send(a2)
        verifyEvent(a2.order, polygonProducer)
    }

    @Test
    fun `multiple events`() = runBlocking<Unit> {
        val a1 = UnionOrderUpdateEvent(randomUnionOrder(randomOrderId(BlockchainDto.ETHEREUM)), null)
        val a2 = UnionOrderUpdateEvent(randomUnionOrder(randomOrderId(BlockchainDto.ETHEREUM)), null)
        val a3 = UnionOrderUpdateEvent(randomUnionOrder(randomOrderId(BlockchainDto.POLYGON)), null)

        handler.send(listOf(a3, a2, a1))

        verifyEvents(listOf(a2.order, a1.order), ethereumProducer)
        verifyEvents(listOf(a3.order), polygonProducer)
    }

    private fun verifyEvent(order: UnionOrder, producer: RaribleKafkaProducer<UnionInternalBlockchainEvent>) {
        coVerify(exactly = 1) {
            producer.send(
                match<KafkaMessage<UnionInternalBlockchainEvent>> {
                    val orderEvent = (it.value as UnionInternalOrderEvent).event as UnionOrderUpdateEvent
                    orderEvent.order == order
                }
            )
        }
    }

    private fun verifyEvents(orders: List<UnionOrder>, producer: RaribleKafkaProducer<UnionInternalBlockchainEvent>) {
        coVerify(exactly = 1) {
            producer.send(
                match<List<KafkaMessage<UnionInternalBlockchainEvent>>> { batch ->
                    val sent = batch.map {
                        val orderEvent = (it.value as UnionInternalOrderEvent).event as UnionOrderUpdateEvent
                        orderEvent.order
                    }
                    sent == orders
                }
            )
        }
    }
}
