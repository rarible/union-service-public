package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.dto.OrderActivityBidDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomEthOrderBidActivity
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
class EthereumActivityEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `ethereum activity event`() = runWithKafka {

        val event: ActivityDto = randomEthOrderBidActivity()

        ethActivityProducer.send(message(event)).ensureSuccess()

        val expectedKey = ActivityIdDto(
            blockchain = BlockchainDto.ETHEREUM,
            value = (event as OrderActivityBidDto).id
        ).fullId()

        Wait.waitAssert {
            val messages = findActivityUpdates(event.id, OrderBidActivityDto::class.java)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(expectedKey)
            assertThat(messages[0].id).isEqualTo(expectedKey)
        }
    }

}