package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.listener.test.data.randomEthLegacyOrderDto
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
class EthereumOrderEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `ethereum - update event`() = runWithKafka {
        val ethOrder = randomEthLegacyOrderDto()
        val ethOrderId = ethOrder.hash.prefixed()
        val dto: OrderEventDto = OrderUpdateEventDto(randomString(), ethOrderId, ethOrder)

        ethOrderProducer.send(message(dto)).ensureSuccess()

        Wait.waitAssert {
            val messages = findEthOrderUpdates(ethOrderId)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(ethOrderId)
            Assertions.assertThat(messages[0].id).isEqualTo(ethOrderId)
        }
    }

}