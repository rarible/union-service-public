package com.rarible.service.kafka

import com.rarible.core.kafka.json.RARIBLE_KAFKA_CLASS_PARAM
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.RetryingBatchErrorHandler
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

class RaribleKafkaListenerContainerFactory<T>(
    private val kafkaBootstrapServers: String,
    private val batchSize: Int,
    concurrency: Int = DEFAULT_CONCURRENCY,
    valueClass: Class<T>,
    groupId: String
) : ConcurrentKafkaListenerContainerFactory<String, T>() {

    init {
        consumerFactory = consumerFactory(valueClass, groupId)
        isBatchListener = true
        setConcurrency(concurrency)
        setBatchErrorHandler(RetryingBatchErrorHandler())
        setRecordFilterStrategy(NullFilteringStrategy<T>())
    }

    private fun <T> consumerFactory(valueClass: Class<T>, groupId: String): DefaultKafkaConsumerFactory<String, T> {
        return DefaultKafkaConsumerFactory(consumerConfigs(valueClass, groupId))
    }

    private fun consumerConfigs(valueClass: Class<*>, groupId: String): Map<String, Any> {
        return mapOf(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBootstrapServers,
            ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS to StringDeserializer::class.java,
            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to UnionKafkaJsonDeserializer::class.java,
            ErrorHandlingDeserializer.KEY_FUNCTION to LoggingDeserializationFailureFunction::class.java,
            ErrorHandlingDeserializer.VALUE_FUNCTION to LoggingDeserializationFailureFunction::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to batchSize,
            RARIBLE_KAFKA_CLASS_PARAM to valueClass
        )
    }

    companion object {
        const val DEFAULT_CONCURRENCY = 10
    }
}
