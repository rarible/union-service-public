package com.rarible.protocol.union.listener.service

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.*
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FlowPreview
@IntegrationTest
class EnrichmentCollectionEventServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var collectionEventService: EnrichmentCollectionEventService

    @Test
    fun `update event`() = runWithKafka {
        val collectionDto = randomUnionCollection()
        val collectionId = collectionDto.id

        collectionEventService.onCollectionUpdated(collectionDto)

        Wait.waitAssert {
            val messages = findCollectionUpdates(collectionDto.id.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(collectionId.fullId())
            assertThat(messages[0].id).isEqualTo(collectionId.fullId())
            assertThat(messages[0].value.collectionId).isEqualTo(collectionId)
            assertThat(messages[0].value.collection).isEqualTo(collectionDto)
        }
    }
}
