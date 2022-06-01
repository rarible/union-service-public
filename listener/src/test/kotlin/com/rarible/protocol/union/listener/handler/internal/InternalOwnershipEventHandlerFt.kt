package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@IntegrationTest
class InternalOwnershipEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `internal ownership event`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ownership = randomEthOwnershipDto(ownershipId)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, emptyList())

        ethOwnershipProducer.send(
            KafkaMessage(
                key = ownershipId.value,
                value = NftOwnershipUpdateEventDto(
                    eventId = randomString(),
                    ownershipId = ownershipId.value,
                    ownership = ownership
                )
            )
        ).ensureSuccess()

        Wait.waitAssert {
            val messages = findOwnershipUpdates(ownershipId.value)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(itemId.fullId())
            Assertions.assertThat(messages[0].id).isEqualTo(itemId.fullId())
            Assertions.assertThat(messages[0].value.ownership.id).isEqualTo(ownershipId)
        }
    }

}
