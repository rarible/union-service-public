package com.rarible.protocol.union.listener.handler.cmp

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.marketplace.generated.whitelabelinternal.dto.MarketplaceChangeEventDto
import com.rarible.marketplace.generated.whitelabelinternal.dto.MarketplaceDeleteEventDto
import com.rarible.marketplace.generated.whitelabelinternal.dto.MarketplaceDto
import com.rarible.marketplace.generated.whitelabelinternal.dto.MarketplaceEventDto
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

@IntegrationTest
internal class CommunityMarketplaceEventHandlerFt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var collectionRepository: CollectionRepository

    @Autowired
    private lateinit var testCommunityMarketplaceEventProducer: RaribleKafkaProducer<MarketplaceEventDto>

    @Test
    fun `marketplace event set priority`() = runBlocking<Unit> {
        val collection1 = randomEnrichmentCollection()
        collectionRepository.save(collection1)

        testCommunityMarketplaceEventProducer.send(
            KafkaMessage(
                key = collection1.collectionId,
                value = MarketplaceChangeEventDto(
                    eventId = UUID.randomUUID().toString(),
                    marketplaceId = "1",
                    data = MarketplaceDto(
                        id = "1",
                        metaRefreshPriority = 10,
                        domain = "test",
                        collectionIds = listOf(collection1.id.toString()),
                    )
                )
            )
        )

        waitAssert {
            assertThat(collectionRepository.get(collection1.id)!!.metaRefreshPriority).isEqualTo(60)
        }
    }

    @Test
    fun `marketplace event unset priority`() = runBlocking<Unit> {
        val collection1 = randomEnrichmentCollection().copy(metaRefreshPriority = 100)
        collectionRepository.save(collection1)

        testCommunityMarketplaceEventProducer.send(
            KafkaMessage(
                key = collection1.collectionId,
                value = MarketplaceChangeEventDto(
                    eventId = UUID.randomUUID().toString(),
                    marketplaceId = "1",
                    data = MarketplaceDto(
                        id = "1",
                        metaRefreshPriority = 0,
                        domain = "test",
                        collectionIds = listOf(collection1.id.toString()),
                    )
                )
            )
        )

        waitAssert {
            assertThat(collectionRepository.get(collection1.id)!!.metaRefreshPriority).isNull()
        }
    }

    @Test
    fun `marketplace event remove`() = runBlocking<Unit> {
        val collection1 = randomEnrichmentCollection().copy(metaRefreshPriority = 100)
        collectionRepository.save(collection1)

        testCommunityMarketplaceEventProducer.send(
            KafkaMessage(
                key = collection1.collectionId,
                value = MarketplaceDeleteEventDto(
                    eventId = UUID.randomUUID().toString(),
                    marketplaceId = "1",
                    data = MarketplaceDto(
                        id = "1",
                        metaRefreshPriority = 100,
                        domain = "test",
                        collectionIds = listOf(collection1.id.toString()),
                    )
                )
            )
        )

        waitAssert {
            assertThat(collectionRepository.get(collection1.id)!!.metaRefreshPriority).isNull()
        }
    }
}
