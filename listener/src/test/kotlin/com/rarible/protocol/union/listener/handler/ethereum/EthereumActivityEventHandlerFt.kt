package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.FlowPreview

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
    //         val messages = findEthActivityUpdates(event.id, OrderBidActivityDto::class.java)
    //         Assertions.assertThat(messages).hasSize(1)
    //         Assertions.assertThat(messages[0].key).isEqualTo(event.id)
    //         Assertions.assertThat(messages[0].id).isEqualTo(event.id)
    //     }
    // }

}