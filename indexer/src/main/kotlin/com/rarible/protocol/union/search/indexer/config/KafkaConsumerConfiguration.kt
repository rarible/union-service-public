package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaContainerFactorySettings
import com.rarible.core.kafka.RaribleKafkaListenerContainerFactory
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.search.indexer.handler.ActivityEventHandler
import com.rarible.protocol.union.search.indexer.handler.CollectionEventHandler
import com.rarible.protocol.union.search.indexer.handler.ItemEventHandler
import com.rarible.protocol.union.search.indexer.handler.OrderEventHandler
import com.rarible.protocol.union.search.indexer.handler.OwnershipEventHandler
import com.rarible.protocol.union.search.indexer.metrics.MetricConsumerBatchEventHandlerFactory
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@Configuration
class KafkaConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val kafkaProperties: KafkaProperties,
    private val metricEventHandlerFactory: MetricConsumerBatchEventHandlerFactory,
    private val kafkaConsumerFactory: RaribleKafkaConsumerFactory
) {
    companion object {
        const val ACTIVITY = "activity"
        const val ORDER = "order"
        const val COLLECTION = "collection"
        const val ITEM = "item"
        const val OWNERSHIP = "ownership"
    }

    private val env = applicationEnvironmentInfo.name

    @Bean
    @ConditionalOnProperty(prefix = "handler.activity", name = ["enabled"], havingValue = "true")
    fun activityWorker(
        handler: ActivityEventHandler,
        activityContainerFactory: RaribleKafkaListenerContainerFactory<ActivityDto>,
    ): ConcurrentMessageListenerContainer<String, ActivityDto> {
        return entityWorker(
            topic = UnionEventTopicProvider.getActivityTopic(env),
            group = consumerGroup(ACTIVITY),
            handler = metricEventHandlerFactory.wrapActivity(handler),
            factory = activityContainerFactory,
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "handler.order", name = ["enabled"], havingValue = "true")
    fun orderWorker(
        handler: OrderEventHandler,
        orderContainerFactory: RaribleKafkaListenerContainerFactory<OrderEventDto>,
    ): ConcurrentMessageListenerContainer<String, OrderEventDto> {
        return entityWorker(
            topic = UnionEventTopicProvider.getOrderTopic(env),
            group = consumerGroup(ORDER),
            handler = metricEventHandlerFactory.wrapOrder(handler),
            factory = orderContainerFactory,
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "handler.collection", name = ["enabled"], havingValue = "true")
    fun collectionWorker(
        handler: CollectionEventHandler,
        collectionContainerFactory: RaribleKafkaListenerContainerFactory<CollectionEventDto>,
    ): ConcurrentMessageListenerContainer<String, CollectionEventDto> {
        return entityWorker(
            topic = UnionEventTopicProvider.getCollectionTopic(env),
            group = consumerGroup(COLLECTION),
            handler = metricEventHandlerFactory.wrapCollection(handler),
            factory = collectionContainerFactory,
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "handler.item", name = ["enabled"], havingValue = "true")
    fun itemWorker(
        handler: ItemEventHandler,
        itemContainerFactory: RaribleKafkaListenerContainerFactory<ItemEventDto>,
    ): ConcurrentMessageListenerContainer<String, ItemEventDto> {
        return entityWorker(
            topic = UnionEventTopicProvider.getItemTopic(env),
            group = consumerGroup(ITEM),
            handler = metricEventHandlerFactory.wrapItem(handler),
            factory = itemContainerFactory,
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "handler.ownership", name = ["enabled"], havingValue = "true")
    fun ownershipWorker(
        handler: OwnershipEventHandler,
        ownershipContainerFactory: RaribleKafkaListenerContainerFactory<OwnershipEventDto>,
    ): ConcurrentMessageListenerContainer<String, OwnershipEventDto> {
        return entityWorker(
            topic = UnionEventTopicProvider.getOwnershipTopic(env),
            group = consumerGroup(OWNERSHIP),
            handler = metricEventHandlerFactory.wrapOwnership(handler),
            factory = ownershipContainerFactory,
        )
    }

    @Bean
    fun activityContainerFactory(): RaribleKafkaListenerContainerFactory<ActivityDto> {
        return RaribleKafkaListenerContainerFactory(
            settings = RaribleKafkaContainerFactorySettings(
                hosts = kafkaProperties.brokerReplicaSet,
                concurrency = kafkaProperties.workerCount,
                batchSize = kafkaProperties.daemon.consumerBatchSize,
                offsetResetStrategy = OffsetResetStrategy.EARLIEST,
                valueClass = ActivityDto::class.java,
                deserializer = UnionKafkaJsonDeserializer::class.java,
            )
        )
    }

    @Bean
    fun orderContainerFactory(): RaribleKafkaListenerContainerFactory<OrderEventDto> {
        return RaribleKafkaListenerContainerFactory(
            settings = RaribleKafkaContainerFactorySettings(
                hosts = kafkaProperties.brokerReplicaSet,
                concurrency = kafkaProperties.workerCount,
                batchSize = kafkaProperties.daemon.consumerBatchSize,
                offsetResetStrategy = OffsetResetStrategy.EARLIEST,
                valueClass = OrderEventDto::class.java,
                deserializer = UnionKafkaJsonDeserializer::class.java,
            )
        )
    }

    @Bean
    fun collectionContainerFactory(): RaribleKafkaListenerContainerFactory<CollectionEventDto> {
        return RaribleKafkaListenerContainerFactory(
            settings = RaribleKafkaContainerFactorySettings(
                hosts = kafkaProperties.brokerReplicaSet,
                concurrency = kafkaProperties.workerCount,
                batchSize = kafkaProperties.daemon.consumerBatchSize,
                offsetResetStrategy = OffsetResetStrategy.EARLIEST,
                valueClass = CollectionEventDto::class.java,
                deserializer = UnionKafkaJsonDeserializer::class.java,
            )
        )
    }

    @Bean
    fun itemContainerFactory(): RaribleKafkaListenerContainerFactory<ItemEventDto> {
        return RaribleKafkaListenerContainerFactory(
            settings = RaribleKafkaContainerFactorySettings(
                hosts = kafkaProperties.brokerReplicaSet,
                concurrency = kafkaProperties.workerCount,
                batchSize = kafkaProperties.daemon.consumerBatchSize,
                offsetResetStrategy = OffsetResetStrategy.EARLIEST,
                valueClass = ItemEventDto::class.java,
                deserializer = UnionKafkaJsonDeserializer::class.java,
            )
        )
    }

    @Bean
    fun ownershipContainerFactory(): RaribleKafkaListenerContainerFactory<OwnershipEventDto> {
        return RaribleKafkaListenerContainerFactory(
            settings = RaribleKafkaContainerFactorySettings(
                hosts = kafkaProperties.brokerReplicaSet,
                concurrency = kafkaProperties.workerCount,
                batchSize = kafkaProperties.daemon.consumerBatchSize,
                offsetResetStrategy = OffsetResetStrategy.EARLIEST,
                valueClass = OwnershipEventDto::class.java,
                deserializer = UnionKafkaJsonDeserializer::class.java,
            )
        )
    }

    fun <T> entityWorker(
        topic: String,
        group: String,
        handler: RaribleKafkaBatchEventHandler<T>,
        factory: RaribleKafkaListenerContainerFactory<T>,
    ): ConcurrentMessageListenerContainer<String, T> {
        val settings = RaribleKafkaConsumerSettings(
            topic = topic,
            group = group,
            async = false,
        )
        return kafkaConsumerFactory.createWorker(settings, handler, factory)
    }

    private fun consumerGroup(suffix: String): String {
        return "protocol.union.search.$suffix"
    }
}
