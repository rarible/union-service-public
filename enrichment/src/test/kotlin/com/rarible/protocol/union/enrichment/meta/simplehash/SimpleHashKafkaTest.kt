package com.rarible.protocol.union.enrichment.meta.simplehash

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.KafkaTestExtension
import com.rarible.core.test.wait.BlockingWait.waitAssert
import com.rarible.protocol.union.enrichment.configuration.EnrichmentConsumerConfiguration
import com.rarible.protocol.union.enrichment.configuration.SimpleHash
import com.rarible.protocol.union.enrichment.configuration.SimpleHashKafka
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.simplehash.v0.nft
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedDeque


@KafkaTest
class SimpleHashKafkaTest {

    @Test
    fun `should send and receive avro message`() = runBlocking<Unit> {

        val kafkaBootstrap = System.getProperty("kafka.hosts")

        val props: UnionMetaProperties = mockk() {
            every { simpleHash } returns SimpleHash(
                kafka = SimpleHashKafka(
                    enabled = true,
                    broker = kafkaBootstrap,
                    topics = listOf("ethereum-${nowMillis().toEpochMilli()}")
                )
            )
        }
        val applicationEnvironmentInfo = ApplicationEnvironmentInfo("test", "localhost")
        val enrichmentConfig = EnrichmentConsumerConfiguration(applicationEnvironmentInfo)
        val factory = enrichmentConfig.simplehashConsumerFactory(props)

        val records = ConcurrentLinkedDeque<nft>()
        val handler = object : RaribleKafkaBatchEventHandler<nft> {
            override suspend fun handle(events: List<nft>) {
                records.addAll(events)
            }

        }
        val worker = enrichmentConfig.simplehashWorker(props, factory, handler)
        worker.start()

        val producer = RaribleKafkaProducer(
            clientId = "test.rarible",
            valueSerializerClass = SHKafkaAvroSerializer::class.java,
            valueClass = nft::class.java,
            defaultTopic = props.simpleHash.kafka.topics.first(),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
            properties = mapOf(
                "auto.register.schemas" to "false",
                "schema.registry.url" to "http://localhost"
            )
        )

        // preparing & sending event
        val event = nft.newBuilder()
            .setNftId("id")
            .build()
        producer.send(
            KafkaMessage(
                key = event.nftId.toString(),
                value = event
            )
        ).ensureSuccess()

        waitAssert {
            assertThat(records).hasSize(1)
            assertThat(records.first).isEqualTo(event)
        }
    }

}