package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthBidOrderDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono

@IntegrationTest
class InternalOrderEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `internal order event`() = runWithKafka {

        // Order without item, we don't need to check Enrichment here
        val order = randomEthBidOrderDto()
            .copy(take = randomEthAssetErc20())

        val orderId = order.hash.prefixed()

        every {
            testEthereumOrderApi.getOrderByHash(any())
        } returns order.toMono()

        ethOrderProducer.send(
            KafkaMessage(
                key = orderId,
                value = OrderUpdateEventDto(
                    eventId = randomString(),
                    orderId = orderId,
                    order = order
                )
            )
        )

        waitAssert {
            val messages = findOrderUpdates(orderId)
            assertThat(messages).hasSize(1)
        }
        verify {
            testEthereumOrderApi.getOrderByHash(order.hash.prefixed())
        }
        confirmVerified(testEthereumOrderApi)
    }
}
