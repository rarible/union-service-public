package com.rarible.protocol.union.listener.wrapped

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthLegacyBidOrderDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
class WrappedOrderEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `wrapped order event`() = runWithKafka {

        // Order without item, we don't need to check Enrichment here
        val order = randomEthLegacyBidOrderDto()
            .copy(take = randomEthAssetErc20())

        val orderId = order.hash.prefixed()

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