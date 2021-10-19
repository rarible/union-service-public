package com.rarible.protocol.union.listener.handler.tezos

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.tezos.dto.OrderEventDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomTezosAssetXtz
import com.rarible.protocol.union.test.data.randomTezosOrderDto
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
class TezosOrderEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `tezos order update event`() = runWithKafka {
        // In this test we don't want to check updates, triggered by order - so here we set ignored assets
        val tezosOrder = randomTezosOrderDto()
            .copy(make = randomTezosAssetXtz(), take = randomTezosAssetXtz())

        val tezosOrderId = tezosOrder.hash
        val expectedOrderId = OrderIdDto(BlockchainDto.TEZOS, tezosOrderId)
        val dto = OrderEventDto(OrderEventDto.Type.UPDATE, randomString(), tezosOrderId, tezosOrder)

        Thread.sleep(3000)
        tezosOrderProducer.send(message(dto)).ensureSuccess()

        Wait.waitAssert {
            val messages = findOrderUpdates(tezosOrderId)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(expectedOrderId.fullId())
            assertThat(messages[0].id).isEqualTo(expectedOrderId.fullId())
            assertThat(messages[0].value.orderId).isEqualTo(expectedOrderId)
        }
    }

}