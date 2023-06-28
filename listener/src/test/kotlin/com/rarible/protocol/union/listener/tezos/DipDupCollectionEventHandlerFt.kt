package com.rarible.protocol.union.listener.tezos

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.integration.tezos.data.randomDipDupCollectionEvent
import com.rarible.protocol.union.integration.tezos.data.randomTzktContract
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.tzkt.client.CollectionClient
import com.rarible.tzkt.model.CollectionType
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class DipDupCollectionEventHandlerFt : AbstractDipDupIntegrationTest() {

    @Autowired
    private lateinit var tzktCollectionClient: CollectionClient

    @Test
    @Disabled("Works locally, fix under PT-953")
    fun `should send collection event to outgoing topic`() = runBlocking {

        val collectionId = randomString()
        val collectionEvent = randomDipDupCollectionEvent(collectionId)

        val tzktCollection = randomTzktContract(collectionId)
        coEvery { tzktCollectionClient.collection(collectionId) } returns tzktCollection
        coEvery { tzktCollectionClient.collectionType(collectionId) } returns CollectionType.MT

        dipDupCollectionProducer.send(
            KafkaMessage(
                key = collectionId,
                value = collectionEvent
            )
        ).ensureSuccess()

        waitAssert {
            val messages = findCollectionUpdates(collectionId)
            assertThat(messages).hasSize(1)
        }
    }
}
