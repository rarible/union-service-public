package com.rarible.protocol.union.listener.tezos

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.kafka.KafkaMessage
import com.rarible.dipdup.listener.model.DipDupItemMetaEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.service.ItemMetaService
import com.rarible.protocol.union.integration.tezos.data.randomTezosTzktItemDto
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coVerify
import org.junit.jupiter.api.Test
import java.util.*

@IntegrationTest
class DipDupItemMetaEventHandlerFt : AbstractDipDupIntegrationTest() {

    @MockkBean
    private lateinit var itemMetaService: ItemMetaService

    @Test
    fun `should send dipdup order to outgoing topic`() = runWithKafka<Unit> {

        val item = randomTezosTzktItemDto()
        val itemDto = ItemIdDto(BlockchainDto.TEZOS, item.itemId())
        val event = DipDupItemMetaEvent(id = UUID.randomUUID(), itemId = item.itemId(), type = "UPDATE")

        dipDupItemMetaProducer.send(
            KafkaMessage(
                key = item.itemId(),
                value = event
            )
        ).ensureSuccess()

        waitAssert {
            coVerify(atLeast = 1) {
                itemMetaService.schedule(itemDto, any(), any())
            }
        }
    }
}
