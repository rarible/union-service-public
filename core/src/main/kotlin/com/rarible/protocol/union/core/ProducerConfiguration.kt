package com.rarible.protocol.union.core

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [ProducerProperties::class])
class ProducerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: ProducerProperties
) {

    private val env = applicationEnvironmentInfo.name
    private val producerBrokerReplicaSet = properties.brokerReplicaSet

    @Bean
    fun itemEventProducer(): RaribleKafkaProducer<ItemEventDto> {
        val itemTopic = UnionEventTopicProvider.getItemTopic(env)
        return createUnionProducer(Entity.ITEM, itemTopic, ItemEventDto::class.java)
    }

    @Bean
    fun ownershipEventProducer(): RaribleKafkaProducer<OwnershipEventDto> {
        val ownershipTopic = UnionEventTopicProvider.getOwnershipTopic(env)
        return createUnionProducer(Entity.OWNERSHIP, ownershipTopic, OwnershipEventDto::class.java)
    }

    @Bean
    fun orderEventProducer(): RaribleKafkaProducer<OrderEventDto> {
        val orderTopic = UnionEventTopicProvider.getOrderTopic(env)
        return createUnionProducer(Entity.ORDER, orderTopic, OrderEventDto::class.java)
    }

    @Bean
    fun activityEventProducer(): RaribleKafkaProducer<ActivityDto> {
        val activityTopic = UnionEventTopicProvider.getActivityTopic(env)
        return createUnionProducer(Entity.ACTIVITY, activityTopic, ActivityDto::class.java)
    }

    private fun <T> createUnionProducer(clientSuffix: String, topic: String, type: Class<T>): RaribleKafkaProducer<T> {
        return RaribleKafkaProducer(
            clientId = "${env}.protocol-union-listener.${clientSuffix}",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = type,
            defaultTopic = topic,
            bootstrapServers = producerBrokerReplicaSet
        )
    }

}