package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelListDto
import com.rarible.protocol.union.dto.FlowOrderCancelListActivityDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
class FlowActivityEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `flow activity event`() = runWithKafka {

        val event: FlowActivityDto = FlowNftOrderActivityCancelListDto(
            id = randomString(),
            date = nowMillis(),
            hash = randomString(),
            maker = randomString(),
            make = FlowAssetFungibleDto(randomString(), randomBigDecimal()),
            take = FlowAssetFungibleDto(randomString(), randomBigDecimal()),
            price = randomBigDecimal()
        )

        flowActivityProducer.send(message(event)).ensureSuccess()

        Wait.waitAssert {
            val messages = findActivityUpdates(event.id, FlowOrderCancelListActivityDto::class.java)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(event.id)
            Assertions.assertThat(messages[0].id).isEqualTo(event.id)
        }
    }

}