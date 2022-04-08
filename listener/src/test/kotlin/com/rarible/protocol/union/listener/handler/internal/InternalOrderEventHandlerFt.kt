package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthLegacyBidOrderDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono

@IntegrationTest
class InternalOrderEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `internal order event`() = runWithKafka {

        // Order without item, we don't need to check Enrichment here
        val order = randomEthLegacyBidOrderDto()
            .copy(take = randomEthAssetErc20())

        val orderId = order.hash.prefixed()

        coEvery { testEthereumOrderApi.getOrderByHash(orderId) } returns order.toMono()

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
