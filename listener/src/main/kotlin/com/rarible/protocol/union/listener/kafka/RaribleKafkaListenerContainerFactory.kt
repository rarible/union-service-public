package com.rarible.protocol.union.listener.kafka

import com.rarible.core.kafka.json.RARIBLE_KAFKA_CLASS_PARAM
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

class RaribleKafkaListenerContainerFactory<T>(
    private val kafkaBootstrapServers: String,
    private val batchSize: Int,
    private val offsetResetStrategy: OffsetResetStrategy = OffsetResetStrategy.LATEST,
    concurrency: Int = DEFAULT_CONCURRENCY,
    valueClass: Class<T>,
    batch: Boolean = true,
) : ConcurrentKafkaListenerContainerFactory<String, T>() {

    init {
        consumerFactory = consumerFactory(valueClass)
        isBatchListener = batch
        setConcurrency(concurrency)
        if (batch) {
            setCommonErrorHandler(DefaultErrorHandler())
        }
        setRecordFilterStrategy(NullFilteringStrategy<T>())
    }

    private fun <T> consumerFactory(valueClass: Class<T>): DefaultKafkaConsumerFactory<String, T> {
        return DefaultKafkaConsumerFactory(consumerConfigs(valueClass))
    }

    private fun consumerConfigs(valueClass: Class<*>): Map<String, Any> {
        return mapOf(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBootstrapServers,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to offsetResetStrategy.name.lowercase(),
            ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS to StringDeserializer::class.java,
            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to if (valueClass == String::class.java)
                StringDeserializer::class.java else UnionKafkaJsonDeserializer::class.java,
            ErrorHandlingDeserializer.KEY_FUNCTION to LoggingDeserializationFailureFunction::class.java,
            ErrorHandlingDeserializer.VALUE_FUNCTION to LoggingDeserializationFailureFunction::class.java,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to batchSize,
            RARIBLE_KAFKA_CLASS_PARAM to valueClass
        )
    }

    companion object {
        const val DEFAULT_CONCURRENCY = 10
    }
}
