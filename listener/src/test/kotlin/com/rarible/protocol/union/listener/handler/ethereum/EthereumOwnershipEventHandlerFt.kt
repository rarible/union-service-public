package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.NftDeletedOwnershipDto
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.listener.test.data.randomEthNftOwnershipDto
import com.rarible.protocol.union.listener.test.data.randomEthOwnershipId
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import scalether.domain.Address

@FlowPreview
@IntegrationTest
class EthereumOwnershipEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `ethereum - update event`() = runWithKafka {
        val ethOwnership = randomEthNftOwnershipDto()
        val dto: NftOwnershipEventDto = NftOwnershipUpdateEventDto(randomString(), ethOwnership.id, ethOwnership)

        ethOwnershipProducer.send(message(dto)).ensureSuccess()

        Wait.waitAssert() {
            val messages = findEthOwnershipUpdates(ethOwnership.id)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(ethOwnership.id)
            Assertions.assertThat(messages[0].id).isEqualTo(ethOwnership.id)
        }
    }

    @Test
    fun `ethereum - delete event`() = runWithKafka {

        val ethOwnershipId = randomEthOwnershipId()

        val deletedDto = NftDeletedOwnershipDto(
            ethOwnershipId.value,
            Address.apply(ethOwnershipId.token.value),
            ethOwnershipId.tokenId,
            Address.apply(ethOwnershipId.owner.value)
        )

        val dto: NftOwnershipEventDto = NftOwnershipDeleteEventDto(randomString(), ethOwnershipId.value, deletedDto)

        ethOwnershipProducer.send(message(dto)).ensureSuccess()

        Wait.waitAssert() {
            val messages = findEthOwnershipDeletions(ethOwnershipId.value)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(ethOwnershipId.value)
            Assertions.assertThat(messages[0].id).isEqualTo(ethOwnershipId.value)
        }
    }

}
