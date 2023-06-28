package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
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
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
        handler: ActivityEventHandler
    ): RaribleKafkaConsumerWorker<ActivityDto> {
        return entityWorker(
            topic = UnionEventTopicProvider.getActivityTopic(env),
            group = consumerGroup(ACTIVITY),
            valueClass = ActivityDto::class.java,
            handler = metricEventHandlerFactory.wrapActivity(handler)
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "handler.order", name = ["enabled"], havingValue = "true")
    fun orderWorker(
        handler: OrderEventHandler
    ): RaribleKafkaConsumerWorker<OrderEventDto> {
        return entityWorker(
            topic = UnionEventTopicProvider.getOrderTopic(env),
            group = consumerGroup(ORDER),
            valueClass = OrderEventDto::class.java,
            handler = metricEventHandlerFactory.wrapOrder(handler)
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "handler.collection", name = ["enabled"], havingValue = "true")
    fun collectionWorker(
        handler: CollectionEventHandler
    ): RaribleKafkaConsumerWorker<CollectionEventDto> {
        return entityWorker(
            topic = UnionEventTopicProvider.getCollectionTopic(env),
            group = consumerGroup(COLLECTION),
            valueClass = CollectionEventDto::class.java,
            handler = metricEventHandlerFactory.wrapCollection(handler)
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "handler.item", name = ["enabled"], havingValue = "true")
    fun itemWorker(
        handler: ItemEventHandler
    ): RaribleKafkaConsumerWorker<ItemEventDto> {
        return entityWorker(
            topic = UnionEventTopicProvider.getItemTopic(env),
            group = consumerGroup(ITEM),
            valueClass = ItemEventDto::class.java,
            handler = metricEventHandlerFactory.wrapItem(handler)
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "handler.ownership", name = ["enabled"], havingValue = "true")
    fun ownershipWorker(
        handler: OwnershipEventHandler,
    ): RaribleKafkaConsumerWorker<OwnershipEventDto> {
        return entityWorker(
            topic = UnionEventTopicProvider.getOwnershipTopic(env),
            group = consumerGroup(OWNERSHIP),
            valueClass = OwnershipEventDto::class.java,
            handler = metricEventHandlerFactory.wrapOwnership(handler)
        )
    }

    fun <T> entityWorker(
        topic: String,
        group: String,
        valueClass: Class<T>,
        handler: RaribleKafkaBatchEventHandler<T>,
        workers: Int = kafkaProperties.workerCount,
        batchSize: Int = kafkaProperties.daemon.consumerBatchSize,
    ): RaribleKafkaConsumerWorker<T> {
        val settings = RaribleKafkaConsumerSettings(
            hosts = kafkaProperties.brokerReplicaSet,
            topic = topic,
            group = group,
            concurrency = workers,
            batchSize = batchSize,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = valueClass
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }

    private fun consumerGroup(suffix: String): String {
        return "protocol.union.search.$suffix"
    }
}
