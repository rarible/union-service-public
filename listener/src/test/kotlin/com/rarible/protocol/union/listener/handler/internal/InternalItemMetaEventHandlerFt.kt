package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.solana.dto.TokenMetaDto
import com.rarible.protocol.solana.dto.TokenMetaUpdateEventDto
import com.rarible.protocol.union.enrichment.converter.EnrichedMetaConverter
import com.rarible.protocol.union.integration.solana.converter.SolanaItemMetaConverter
import com.rarible.protocol.union.integration.solana.data.randomSolanaItemId
import com.rarible.protocol.union.integration.solana.data.randomSolanaTokenDto
import com.rarible.protocol.union.integration.solana.data.randomTokenMeta
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

@IntegrationTest
class InternalItemMetaEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `internal item meta event - should save meta and send item update event`() = runWithKafka {
        val itemId = randomSolanaItemId()
        val item = randomSolanaTokenDto(itemId)
        val tokenMeta: TokenMetaDto = randomTokenMeta()

        every { testSolanaTokenControllerApi.getTokenByAddress(itemId.value) } returns Mono.just(item)

        solanaTokenMetaEventProducer.send(
            KafkaMessage(
                key = itemId.value,
                value = TokenMetaUpdateEventDto(
                    tokenAddress = itemId.value,
                    tokenMeta = tokenMeta
                )
            )
        )

        waitAssert {
            val unionMeta = SolanaItemMetaConverter.convert(tokenMeta)
            assertThat(unionMetaService.getAvailableMeta(listOf(itemId)))
                .isEqualTo(mapOf(itemId to unionMeta))

            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
            assertThat(messages[0].value.item.meta).isEqualTo(EnrichedMetaConverter.convert(unionMeta))
        }
    }

}
