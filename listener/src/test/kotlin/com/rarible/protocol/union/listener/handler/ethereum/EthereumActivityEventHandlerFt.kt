package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.union.dto.UnionOrderBidActivityDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomEthOrderBidActivity
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

@FlowPreview
@IntegrationTest
class EthereumActivityEventHandlerFt : AbstractIntegrationTest() {

    // @Test
    // fun `ethereum activity event`() = runWithKafka {
    //
    //     val event: ActivityDto = randomEthOrderBidActivity()
    //
    //     ethActivityProducer.send(message(event)).ensureSuccess()
    //
    //     Wait.waitAssert(Duration.ofMillis(5000)) {
    //         val messages = findEthActivityUpdates(event.id, UnionOrderBidActivityDto::class.java)
    //         Assertions.assertThat(messages).hasSize(1)
    //         Assertions.assertThat(messages[0].key).isEqualTo(event.id)
    //         Assertions.assertThat(messages[0].id).isEqualTo(event.id)
    //     }
    // }

}