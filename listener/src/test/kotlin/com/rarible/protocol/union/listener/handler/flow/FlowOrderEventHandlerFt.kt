package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomFlowV1OrderDto
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
@Disabled // TODO enable after enrichment implemented
class FlowOrderEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `flow order update event`() = runWithKafka {
        val flowOrder = randomFlowV1OrderDto()
        val flowOrderId = flowOrder.id.toString()
        val dto: FlowOrderEventDto = FlowOrderUpdateEventDto(randomString(), flowOrderId, flowOrder)

        flowOrderProducer.send(message(dto)).ensureSuccess()

        Wait.waitAssert {
            val messages = findFlowOrderUpdates(flowOrderId)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(flowOrderId)
            Assertions.assertThat(messages[0].id).isEqualTo(flowOrderId)
        }
    }

}