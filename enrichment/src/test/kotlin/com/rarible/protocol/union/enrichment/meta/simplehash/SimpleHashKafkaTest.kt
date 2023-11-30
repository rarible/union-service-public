package com.rarible.protocol.union.enrichment.meta.simplehash

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.Compression
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.ext.KafkaCleanup
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.KafkaTestExtension
import com.rarible.core.test.wait.BlockingWait.waitAssert
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.CommonMetaProperties
import com.rarible.protocol.union.enrichment.configuration.SimpleHash
import com.rarible.protocol.union.enrichment.configuration.SimpleHashKafka
import com.rarible.protocol.union.enrichment.configuration.SimplehashConsumerConfiguration
import com.rarible.simplehash.client.subcriber.SimplehashKafkaAvroSerializer
import com.simplehash.v0.nft
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedDeque

@Disabled("Works locally, but fails in jenkins")
@KafkaTest
@KafkaCleanup
class SimpleHashKafkaTest {

    @Test
    fun `should send and receive avro message`() = runBlocking<Unit> {

        val kafkaBootstrap = System.getProperty("kafka.hosts")
        val topic = "ethereum-${nowMillis().toEpochMilli()}"

        val props: CommonMetaProperties = mockk() {
            every { simpleHash } returns SimpleHash(
                kafka = SimpleHashKafka(
                    enabled = true,
                    broker = kafkaBootstrap,
                    topics = listOf(topic)
                ),
                supported = setOf(BlockchainDto.ETHEREUM)
            )
        }
        val applicationEnvironmentInfo = ApplicationEnvironmentInfo("test", "localhost")
        val enrichmentConfig = SimplehashConsumerConfiguration(applicationEnvironmentInfo)
        val factory = enrichmentConfig.simplehashConsumerFactory(props)

        val records = ConcurrentLinkedDeque<nft>()
        val handler = object : RaribleKafkaBatchEventHandler<nft> {
            override suspend fun handle(events: List<nft>) {
                records.addAll(events)
            }
        }
        val worker = enrichmentConfig.simplehashWorker(props, factory, handler)
        worker.start()

        // There's offsetResetStrategy = OffsetResetStrategy.LATEST, that why better to wait a little bit
        delay(250)

        val producer = RaribleKafkaProducer(
            clientId = "test.rarible",
            valueSerializerClass = SimplehashKafkaAvroSerializer::class.java,
            valueClass = nft::class.java,
            defaultTopic = topic,
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
            properties = mapOf(
                "auto.register.schemas" to "false",
                "schema.registry.url" to "http://localhost"
            ),
            compression = Compression.SNAPPY,
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
