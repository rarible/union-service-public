package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.NftDeletedItemDto
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomEthNftItemDto
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import scalether.domain.Address

@FlowPreview
@IntegrationTest
@Disabled // TODO enable after enrichment implemented
class EthereumItemEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `ethereum item update event`() = runWithKafka {
        val ethItem = randomEthNftItemDto()
        val dto: NftItemEventDto = NftItemUpdateEventDto(randomString(), ethItem.id, ethItem)

        ethItemProducer.send(message(dto)).ensureSuccess()

        Wait.waitAssert {
            val messages = findEthItemUpdates(ethItem.id)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(ethItem.id)
            assertThat(messages[0].id).isEqualTo(ethItem.id)
        }
    }

    @Test
    fun `ethereum item delete event`() = runWithKafka {
        val ethItemId = randomEthItemId()
        val deletedDto = NftDeletedItemDto(
            ethItemId.value,
            Address.apply(ethItemId.token.value),
            ethItemId.tokenId
        )

        val dto: NftItemEventDto = NftItemDeleteEventDto(randomString(), ethItemId.value, deletedDto)

        ethItemProducer.send(message(dto)).ensureSuccess()

        Wait.waitAssert {
            val messages = findEthItemDeletions(ethItemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(ethItemId.value)
            assertThat(messages[0].id).isEqualTo(ethItemId.value)
        }
    }

}