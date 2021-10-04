package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelListDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomFlowCancelListActivityDto
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
class FlowActivityEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `flow activity event`() = runWithKafka {

        val event: FlowActivityDto = randomFlowCancelListActivityDto()

        flowActivityProducer.send(message(event)).ensureSuccess()

        val expectedKey = ActivityIdDto(
            blockchain = BlockchainDto.FLOW,
            value = (event as FlowNftOrderActivityCancelListDto).id
        ).fullId()

        Wait.waitAssert {
            val messages = findFlowActivityUpdates(event.id, OrderCancelListActivityDto::class.java)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(expectedKey)
            Assertions.assertThat(messages[0].id).isEqualTo(expectedKey)
        }
    }

}