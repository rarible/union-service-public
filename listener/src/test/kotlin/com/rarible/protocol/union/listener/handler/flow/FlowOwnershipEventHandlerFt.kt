package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.FlowNftOwnershipDeleteEventDto
import com.rarible.protocol.dto.FlowNftOwnershipUpdateEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomFlowNftOwnershipDto
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration

@FlowPreview
@IntegrationTest
@Disabled // TODO enable after enrichment implemented
class FlowOwnershipEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `flow ownership update event`() = runWithKafka {
        val flowOwnership = randomFlowNftOwnershipDto()
        val dto: FlowOwnershipEventDto =
            FlowNftOwnershipUpdateEventDto(randomString(), flowOwnership.id!!, flowOwnership)

        flowOwnershipProducer.send(message(dto)).ensureSuccess()

        Wait.waitAssert(Duration.ofMinutes(5)) {
            val messages = findFlowOwnershipUpdates(flowOwnership.id!!)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(flowOwnership.id)
            Assertions.assertThat(messages[0].id).isEqualTo(flowOwnership.id)
        }
    }

    @Test
    fun `flow ownership delete event`() = runWithKafka {
        val flowOwnership = randomFlowNftOwnershipDto()
        val dto: FlowOwnershipEventDto =
            FlowNftOwnershipDeleteEventDto(randomString(), flowOwnership.id!!, flowOwnership)

        flowOwnershipProducer.send(message(dto)).ensureSuccess()

        Wait.waitAssert(Duration.ofMinutes(5)) {
            val messages = findFlowOwnershipDeletions(flowOwnership.id!!)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(flowOwnership.id)
            Assertions.assertThat(messages[0].id).isEqualTo(flowOwnership.id)
        }
    }

}
