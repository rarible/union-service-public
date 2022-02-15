package com.rarible.protocol.union.listener.wrapped

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.AuctionsPaginationDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.clearMocks
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono

@IntegrationTest
class WrappedOwnershipEventHandlerFt : AbstractIntegrationTest() {

    @BeforeEach
    fun beforeEach() {
        clearMocks(testEthereumAuctionApi)
    }

    @Test
    fun `wrapped ownership event`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ownership = randomEthOwnershipDto(ownershipId)

        coEvery {
            testEthereumAuctionApi.getAuctionsByItem(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(1)
            )
        } returns AuctionsPaginationDto(emptyList(), null).toMono()

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
