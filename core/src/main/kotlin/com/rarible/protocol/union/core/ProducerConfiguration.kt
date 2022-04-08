package com.rarible.protocol.union.core

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.event.UnionInternalTopicProvider
import com.rarible.protocol.union.core.model.ReconciliationMarkAbstractEvent
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
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
    fun collectionEventProducer(): RaribleKafkaProducer<CollectionEventDto> {
        val collectionTopic = UnionEventTopicProvider.getCollectionTopic(env)
        return createUnionProducer("collection", collectionTopic, CollectionEventDto::class.java)
    }

    @Bean
    fun itemEventProducer(): RaribleKafkaProducer<ItemEventDto> {
        val itemTopic = UnionEventTopicProvider.getItemTopic(env)
        return createUnionProducer("item", itemTopic, ItemEventDto::class.java)
    }

    @Bean
    fun ownershipEventProducer(): RaribleKafkaProducer<OwnershipEventDto> {
        val ownershipTopic = UnionEventTopicProvider.getOwnershipTopic(env)
        return createUnionProducer("ownership", ownershipTopic, OwnershipEventDto::class.java)
    }

    @Bean
    fun orderEventProducer(): RaribleKafkaProducer<OrderEventDto> {
        val orderTopic = UnionEventTopicProvider.getOrderTopic(env)
        return createUnionProducer("order", orderTopic, OrderEventDto::class.java)
    }

    @Bean
    fun activityEventProducer(): RaribleKafkaProducer<ActivityDto> {
        val activityTopic = UnionEventTopicProvider.getActivityTopic(env)
        return createUnionProducer("activity", activityTopic, ActivityDto::class.java)
    }

    @Bean
    fun internalBlockchainEventProducer(): UnionInternalBlockchainEventProducer {
        val producers = HashMap<BlockchainDto, RaribleKafkaProducer<UnionInternalBlockchainEvent>>()
        // We can create producers for all blockchains, even for disabled (just to avoid NPE checks)
        BlockchainDto.values().forEach {
            val producer = createUnionProducer(
                clientSuffix = "blockchain.${it.name.lowercase()}",
                topic = UnionInternalTopicProvider.getInternalBlockchainTopic(env, it),
                type = UnionInternalBlockchainEvent::class.java
            )
            producers[it] = producer
        }
        return UnionInternalBlockchainEventProducer(producers)
    }

    @Bean
    fun reconciliationMarkEventProducer(): RaribleKafkaProducer<ReconciliationMarkAbstractEvent> {
        val topic = UnionInternalTopicProvider.getReconciliationMarkTopic(env)
        return createUnionProducer("reconciliation", topic, ReconciliationMarkAbstractEvent::class.java)
    }

    private fun <T> createUnionProducer(clientSuffix: String, topic: String, type: Class<T>): RaribleKafkaProducer<T> {
        return RaribleKafkaProducer(
            clientId = "${env}.protocol-union-service.${clientSuffix}",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = type,
            defaultTopic = topic,
            bootstrapServers = producerBrokerReplicaSet
        )
    }

}