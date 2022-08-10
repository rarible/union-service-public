package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.AuctionsPaginationDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

@IntegrationTest
class InternalOwnershipEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `internal ownership event`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ownership = randomEthOwnershipDto(ownershipId)

        every {
            testEthereumAuctionApi.getAuctionsByItem(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } returns Mono.just(AuctionsPaginationDto(emptyList(), null))

        ethOwnershipProducer.send(
            KafkaMessage(
                key = ownershipId.value, value = NftOwnershipUpdateEventDto(
                    eventId = randomString(), ownershipId = ownershipId.value, ownership = ownership
                )
            )
        ).ensureSuccess()

        waitAssert {
            val messages = findOwnershipUpdates(ownershipId.value)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(itemId.fullId())
            Assertions.assertThat(messages[0].id).isEqualTo(itemId.fullId())
            Assertions.assertThat(messages[0].value.ownership.id).isEqualTo(ownershipId)
        }
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        verify {
            testEthereumAuctionApi.getAuctionsByItem(
                contract, tokenId.toString(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
        confirmVerified(testEthereumAuctionApi)
    }
}
