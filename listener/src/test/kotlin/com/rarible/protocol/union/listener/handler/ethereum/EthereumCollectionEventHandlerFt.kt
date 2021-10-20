package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.union.core.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomEthCollectionDto
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
class EthereumCollectionEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `ethereum collection update event`() = runWithKafka {
        val ethCollection = randomEthCollectionDto()
        val event: NftCollectionEventDto = NftCollectionUpdateEventDto(randomString(), ethCollection.id, ethCollection)

        val converted = EthCollectionConverter.convert(ethCollection, BlockchainDto.ETHEREUM)

        ethCollectionProducer.send(message(event)).ensureSuccess()

        val expectedKey = converted.id.fullId()

        Wait.waitAssert {
            val messages = findCollectionUpdates(converted.id.value)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(expectedKey)
            Assertions.assertThat(messages[0].id).isEqualTo(expectedKey)
        }
    }
}