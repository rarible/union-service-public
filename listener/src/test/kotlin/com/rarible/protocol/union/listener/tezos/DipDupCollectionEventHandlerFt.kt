package com.rarible.protocol.union.listener.tezos

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.integration.tezos.data.randomDipDupCollectionEvent
import com.rarible.protocol.union.listener.test.IntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@IntegrationTest
class DipDupCollectionEventHandlerFt : AbstractDipDupIntegrationTest() {

    @Test
    fun `should send collection event to outgoing topic`() = runWithKafka {

        val collectionId = randomString()
        val collectionEvent = randomDipDupCollectionEvent(collectionId)

        dipDupCollectionProducer.send(
            KafkaMessage(
                key = collectionId,
                value = collectionEvent
            )
        ).ensureSuccess()

        waitAssert {
            val messages = findCollectionUpdates(collectionId)
            Assertions.assertThat(messages).hasSize(1)
        }
    }
}
