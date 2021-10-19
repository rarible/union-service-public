package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomEthAssetErc20
import com.rarible.protocol.union.test.data.randomEthLegacyOrderDto
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
class EthereumOrderEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `ethereum order update event`() = runWithKafka {
        // In this test we don't want to check updates, triggered by order - so here we set ignored assets
        val ethOrder = randomEthLegacyOrderDto()
            .copy(make = randomEthAssetErc20(), take = randomEthAssetErc20())

        val ethOrderId = EthConverter.convert(ethOrder.hash)
        val expectedOrderId = OrderIdDto(BlockchainDto.ETHEREUM, ethOrderId)
        val dto: OrderEventDto = OrderUpdateEventDto(randomString(), ethOrderId, ethOrder)

        ethOrderProducer.send(message(dto)).ensureSuccess()

        Wait.waitAssert {
            val messages = findOrderUpdates(ethOrderId)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(expectedOrderId.fullId())
            assertThat(messages[0].id).isEqualTo(expectedOrderId.fullId())
            assertThat(messages[0].value.orderId).isEqualTo(expectedOrderId)
        }
    }

}