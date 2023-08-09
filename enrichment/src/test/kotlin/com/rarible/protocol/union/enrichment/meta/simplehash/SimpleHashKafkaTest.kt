package com.rarible.protocol.union.enrichment.meta.simplehash

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.containers.KafkaTestContainer
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.KafkaTestExtension
import com.rarible.core.test.wait.BlockingWait.waitAssert
import com.rarible.protocol.union.enrichment.configuration.SimpleHash
import com.rarible.protocol.union.enrichment.configuration.SimpleHashKafka
import com.rarible.protocol.union.enrichment.configuration.SimplehashConsumerConfiguration
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.simplehash.client.subcriber.SimplehashKafkaAvroSerializer
import com.simplehash.v0.nft
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.ConcurrentLinkedDeque

//@Disabled("Works locally, but fails in jenkins")
@KafkaTest
class SimpleHashKafkaTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            System.setProperty(
                "META_SIMPLEHASH_KAFKA_USERNAME", "test"
            )
        }
    }


    @Test
    fun `should send and receive avro message`() = runBlocking<Unit> {

        val kafkaBootstrap = System.getProperty("kafka.hosts")
        val topic = "ethereum.nft.v2"

        val props: UnionMetaProperties = mockk() {
            every { simpleHash } returns SimpleHash(
                kafka = SimpleHashKafka(
                    enabled = true,
                    broker = "pkc-3w22w.us-central1.gcp.confluent.cloud:9092",
                    topics = listOf(topic),
                    username = "36DWNZGZ4XF5GCSO",
                    password = "C4SmNsRoTMmqtwj48+YbP4nq5DlIOVgXcgLOD0WWwi0c47Nzm/qwF+dZqFb6OurA",
                )
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
        delay(25000000)

//        val producer = RaribleKafkaProducer(
//            clientId = "test.rarible",
//            valueSerializerClass = SimplehashKafkaAvroSerializer::class.java,
//            valueClass = nft::class.java,
//            defaultTopic = topic,
//            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
//            properties = mapOf(
//                "auto.register.schemas" to "false",
//                "schema.registry.url" to "http://localhost"
//            )
//        )
//
//        // preparing & sending event
//        val event = nft.newBuilder()
//            .setNftId("id")
//            .build()
//        producer.send(
//            KafkaMessage(
//                key = event.nftId.toString(),
//                value = event
//            )
//        ).ensureSuccess()

//        waitAssert {
//            assertThat(records).hasSize(1)
//            assertThat(records.first).isEqualTo(event)
//        }
    }
}
