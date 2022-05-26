package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthBidOrderDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@IntegrationTest
class InternalOrderEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `internal order event`() = runWithKafka {

        // Order without item, we don't need to check Enrichment here
        val order = randomEthBidOrderDto()
            .copy(take = randomEthAssetErc20())

        val orderId = order.hash.prefixed()

        ethereumOrderControllerApiMock.mockGetById(order)

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

        Wait.waitAssert {
            val messages = findOrderUpdates(orderId)
            Assertions.assertThat(messages).hasSize(1)
        }
    }
}
