package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftItemMetaRefreshEventDto
import com.rarible.protocol.union.core.test.TestUnionEventHandler
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.model.MetaDownloadPriority
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class InternalItemMetaEventHandlerFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var downloadTaskEventHandler: TestUnionEventHandler<DownloadTaskEvent>

    @Test
    fun `internal item meta event`() = runBlocking {
        val itemId = randomEthItemId()

        ethItemMetaProducer.send(
            KafkaMessage(
                key = itemId.value,
                value = NftItemMetaRefreshEventDto(
                    eventId = randomString(),
                    itemId = itemId.value,
                )
            )
        ).ensureSuccess()

        waitAssert {
            val events = downloadTaskEventHandler.events.toList()
            assertThat(events).hasSize(1)
            assertThat(events[0].id).isEqualTo(itemId.toString())
            assertThat(events[0].priority).isEqualTo(MetaDownloadPriority.HIGH)
        }
    }
}
