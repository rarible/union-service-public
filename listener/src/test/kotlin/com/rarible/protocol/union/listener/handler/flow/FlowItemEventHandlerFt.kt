package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.FlowNftDeletedItemDto
import com.rarible.protocol.dto.FlowNftItemDeleteEventDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomFlowItemId
import com.rarible.protocol.union.test.data.randomFlowNftItemDto
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
class FlowItemEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `flow item update event`() = runWithKafka {
        val flowItem = randomFlowNftItemDto()
        val dto: FlowNftItemEventDto = FlowNftItemUpdateEventDto(randomString(), flowItem.id, flowItem)

        flowItemProducer.send(message(dto)).ensureSuccess()

        Wait.waitAssert {
            val messages = findFlowItemUpdates(flowItem.id)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(flowItem.id)
            Assertions.assertThat(messages[0].id).isEqualTo(flowItem.id)
        }
    }

    @Test
    fun `item delete event`() = runWithKafka {
        val flowItemId = randomFlowItemId()
        val deletedDto = FlowNftDeletedItemDto(
            flowItemId.value,
            flowItemId.token.value,
            flowItemId.tokenId.toLong()
        )

        val dto: FlowNftItemEventDto = FlowNftItemDeleteEventDto(randomString(), flowItemId.value, deletedDto)

        flowItemProducer.send(message(dto)).ensureSuccess()

        Wait.waitAssert {
            val messages = findFlowItemDeletions(flowItemId.value)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(flowItemId.value)
            Assertions.assertThat(messages[0].id).isEqualTo(flowItemId.value)
        }
    }

}