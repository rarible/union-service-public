package com.rarible.protocol.union.worker.kafka

import com.rarible.core.test.ext.KafkaTestExtension
import com.rarible.protocol.union.core.ProducerProperties
import com.rarible.protocol.union.worker.AbstractIntegrationTest
import com.rarible.protocol.union.worker.IntegrationTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.util.UUID

@IntegrationTest
internal class LagServiceIt : AbstractIntegrationTest() {
    private val consumerGroup = UUID.randomUUID().toString()
    private val properties = mapOf(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
        ConsumerConfig.GROUP_ID_CONFIG to consumerGroup,
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
    )

    @Autowired
    private lateinit var kafkaConsumer: KafkaConsumer<String, String>

    @Autowired
    private lateinit var producerProperties: ProducerProperties

    private lateinit var lagService: LagService

    @BeforeEach
    fun before() {
        lagService = LagService(
            kafkaConsumer = kafkaConsumer,
            bootstrapServers = producerProperties.brokerReplicaSet,
            metaConsumerGroup = consumerGroup,
            refreshTopic = "test-topic",
            maxLag = 2
        )
    }

    @Test
    fun `check lag ok`() = runBlocking<Unit> {
        assertThat(lagService.isLagOk()).isTrue

        val consumer = KafkaConsumer<String, String>(properties)
        val consumer2 = KafkaConsumer<String, String>(properties)
        val producer = KafkaProducer<String, String>(properties)
        consumer.subscribe(listOf("test-topic"))
        consumer.poll(Duration.ofMillis(5000))
        consumer2.subscribe(listOf("test-topic2"))
        consumer2.poll(Duration.ofMillis(5000))

        assertThat(lagService.isLagOk()).isTrue

        withContext(Dispatchers.IO) {
            producer.send(ProducerRecord("test-topic", "key1", "test1")).get()
            producer.send(ProducerRecord("test-topic", "key2", "test2")).get()
            producer.send(ProducerRecord("test-topic", "key2", "test3")).get()
            producer.send(ProducerRecord("test-topic", "key2", "test4")).get()
            producer.send(ProducerRecord("test-topic", "key2", "test5")).get()
            producer.send(ProducerRecord("test-topic", "key2", "test6")).get()
            producer.send(ProducerRecord("test-topic2", "key2", "test7")).get()
            producer.flush()
        }

        val records = consumer.poll(Duration.ofSeconds(1))
        consumer.commitSync()
        assertThat(records.records("test-topic").toList()[0].value()).isEqualTo("test1")

        assertThat(lagService.isLagOk()).isFalse
        consumer.poll(Duration.ofSeconds(1))
        consumer.poll(Duration.ofSeconds(1))
        consumer.poll(Duration.ofSeconds(1))
        consumer.poll(Duration.ofSeconds(1))
        consumer.commitSync()
        assertThat(lagService.isLagOk()).isTrue
    }
}