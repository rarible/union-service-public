package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomFlowFungibleAsset
import com.rarible.protocol.union.test.data.randomFlowV1OrderDto
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
class FlowOrderEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `flow order update event`() = runWithKafka {
        // In this test we don't want to check updates, triggered by order - so here we set ignored assets
        val flowOrder = randomFlowV1OrderDto()
            .copy(make = randomFlowFungibleAsset(), take = randomFlowFungibleAsset())

        val flowOrderId = flowOrder.id.toString()
        val expectedOrderId = OrderIdDto(BlockchainDto.FLOW, flowOrderId)
        val dto: FlowOrderEventDto = FlowOrderUpdateEventDto(randomString(), flowOrderId, flowOrder)

        flowOrderProducer.send(message(dto)).ensureSuccess()

        Wait.waitAssert {
            val messages = findFlowOrderUpdates(flowOrderId)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(expectedOrderId.fullId())
            assertThat(messages[0].id).isEqualTo(expectedOrderId.fullId())
            assertThat(messages[0].value.orderId).isEqualTo(expectedOrderId)
        }
    }

}