package com.rarible.protocol.union.listener.wrapped

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemBurnActivity
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@IntegrationTest
class WrappedActivityEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `wrapped activity event`() = runWithKafka {

        val activity = randomEthItemBurnActivity()

        ethActivityProducer.send(
            KafkaMessage(
                key = activity.id,
                value = activity
            )
        ).ensureSuccess()

        Wait.waitAssert {
            val messages = findActivityUpdates(activity.id, BurnActivityDto::class.java)
            Assertions.assertThat(messages).hasSize(1)
        }
    }
}
