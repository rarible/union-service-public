package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@IntegrationTest
class InternalItemEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `internal item event`() = runBlocking {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)

        ethItemProducer.send(
            KafkaMessage(
                key = itemId.value,
                value = NftItemUpdateEventDto(
                    eventId = randomString(),
                    itemId = itemId.value,
                    item = ethItem
                )
            )
        ).ensureSuccess()

        waitAssert {
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].itemId).isEqualTo(itemId)
        }
    }

}
