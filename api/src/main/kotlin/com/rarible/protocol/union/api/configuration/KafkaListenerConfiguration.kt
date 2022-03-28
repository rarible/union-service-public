package com.rarible.api.configuration

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.protocol.union.core.domain.Item

import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.service.activity.BatchActivityEventHandler
import com.rarible.service.kafka.RaribleKafkaListenerContainerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import java.net.InetAddress

@Configuration
@EnableKafka
class KafkaListenerConfiguration(
    @Value("\${rarible.kafka.hosts}") private val kafkaBootstrapServers: String,
    @Value("\${rarible.kafka.batchSize:100}") private val batchSize: Int
) {

    @Bean
    fun batchActivityEventHandler(eventHandler: ConsumerEventHandler<ActivityDto>): BatchActivityEventHandler {
        return BatchActivityEventHandler(eventHandler)
    }

    @Bean
    fun unionActivityListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, ActivityDto> {
        return unionContainerFactory(ActivityDto::class.java)
    }

    private fun <T> unionContainerFactory(
        valueClass: Class<T>
    ): ConcurrentKafkaListenerContainerFactory<String, T> =
        containerFactory(
            valueClass = valueClass,
            groupId = "marketplace-api.activity.union.${InetAddress.getLocalHost().hostAddress}"
        )

    private fun <T> containerFactory(
        valueClass: Class<T>,
        groupId: String
    ): ConcurrentKafkaListenerContainerFactory<String, T> {
        return RaribleKafkaListenerContainerFactory(
            valueClass = valueClass,
            groupId = groupId,
            kafkaBootstrapServers = kafkaBootstrapServers,
            batchSize = batchSize
        )
    }

    @Bean
    fun itemTopicListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Item> {
        return marketplaceContainerFactory(Item::class.java)
    }


    private fun <T> marketplaceContainerFactory(
        valueClass: Class<T>
    ): ConcurrentKafkaListenerContainerFactory<String, T> =
        containerFactory(
            valueClass = valueClass,
            groupId = "marketplace-api.update.event.listener.${InetAddress.getLocalHost().hostAddress}"
        )
}
