package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.search.indexer.metrics.MetricConsumerBatchEventHandlerFactory
import com.rarible.protocol.union.subscriber.UnionEventsConsumerFactory
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class KafkaConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val kafkaProperties: KafkaProperties,
    private val meterRegistry: MeterRegistry,
    private val metricEventHandlerFactory: MetricConsumerBatchEventHandlerFactory
) {
    companion object {
        const val ACTIVITY = "activity"
        const val ORDER = "order"
        const val COLLECTION = "collection"
        const val ITEM = "item"
        const val OWNERSHIP = "ownership"
    }

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val consumerFactory = UnionEventsConsumerFactory(kafkaProperties.brokerReplicaSet, host, env)

    @Bean
    @ConditionalOnProperty(prefix = "handler.activity", name = ["enabled"], havingValue = "true")
    fun activityWorker(
        handler: ConsumerBatchEventHandler<ActivityDto>
    ): ConsumerWorkerHolder<ActivityDto> {
        val wrappedHandler = metricEventHandlerFactory.wrapActivity(handler)
        val workers = (1..kafkaProperties.workerCount).map { index ->
            val consumer = consumerFactory.createActivityConsumer(consumerGroup(ACTIVITY))
            ConsumerBatchWorker(
                consumer = consumer,
                eventHandler = wrappedHandler,
                workerName = worker(ACTIVITY, index),
                properties = kafkaProperties.daemon,
                retryProperties = RetryProperties(attempts = Integer.MAX_VALUE, delay = Duration.ofMillis(1000)),
                meterRegistry = meterRegistry,
            )
        }
        return ConsumerWorkerHolder(workers)
    }

    @Bean
    @ConditionalOnProperty(prefix = "handler.order", name = ["enabled"], havingValue = "true")
    fun orderWorker(
        handler: ConsumerBatchEventHandler<OrderEventDto>
    ): ConsumerWorkerHolder<OrderEventDto> {
        val workers = (1..kafkaProperties.workerCount).map { index ->
            val wrappedHandler = metricEventHandlerFactory.wrapOrder(handler)
            val consumer = consumerFactory.createOrderConsumer(consumerGroup(ORDER))
            ConsumerBatchWorker(
                consumer = consumer,
                eventHandler = wrappedHandler,
                workerName = worker(ORDER, index),
                properties = kafkaProperties.daemon,
                retryProperties = RetryProperties(attempts = Integer.MAX_VALUE, delay = Duration.ofMillis(1000)),
                meterRegistry = meterRegistry,
            )
        }
        return ConsumerWorkerHolder(workers)
    }

    @Bean
    @ConditionalOnProperty(prefix = "handler.collection", name = ["enabled"], havingValue = "true")
    fun collectionWorker(handler: ConsumerBatchEventHandler<CollectionEventDto>): ConsumerWorkerHolder<CollectionEventDto> {
        val workers = (1..kafkaProperties.workerCount).map { i ->
            val wrappedHandler = metricEventHandlerFactory.wrapCollection(handler)
            val consumer = consumerFactory.createCollectionConsumer(consumerGroup(COLLECTION))
            ConsumerBatchWorker(
                consumer = consumer,
                eventHandler = wrappedHandler,
                workerName = worker(COLLECTION, i),
                properties = kafkaProperties.daemon,
                retryProperties = RetryProperties(attempts = Int.MAX_VALUE, delay = Duration.ofSeconds(1L)),
                meterRegistry = meterRegistry
            )
        }
        return ConsumerWorkerHolder(workers)
    }

    @Bean
    @ConditionalOnProperty(prefix = "handler.item", name = ["enabled"], havingValue = "true")
    fun itemWorker(handler: ConsumerBatchEventHandler<ItemEventDto>): ConsumerWorkerHolder<ItemEventDto> {
        val workers = (1..kafkaProperties.workerCount).map { i ->
            val wrappedHandler = metricEventHandlerFactory.wrapItem(handler)
            val consumer = consumerFactory.createItemConsumer(consumerGroup(ITEM))
            ConsumerBatchWorker(
                consumer = consumer,
                eventHandler = wrappedHandler,
                workerName = worker(ITEM, i),
                properties = kafkaProperties.daemon,
                retryProperties = RetryProperties(attempts = Int.MAX_VALUE, delay = Duration.ofSeconds(1L)),
                meterRegistry = meterRegistry
            )
        }
        return ConsumerWorkerHolder(workers)
    }

    @Bean
    @ConditionalOnProperty(prefix = "handler.ownership", name = ["enabled"], havingValue = "true")
    fun ownershipWorker(
        handler: ConsumerBatchEventHandler<OwnershipEventDto>,
    ): ConsumerWorkerHolder<OwnershipEventDto> {
        val wrappedHandler = metricEventHandlerFactory.wrapOwnership(handler)
        val workers = (1..kafkaProperties.workerCount).map { index ->
            val consumer = consumerFactory.createOwnershipConsumer(consumerGroup(OWNERSHIP))
            ConsumerBatchWorker(
                consumer = consumer,
                eventHandler = wrappedHandler,
                workerName = worker(OWNERSHIP, index),
                properties = kafkaProperties.daemon,
                retryProperties = RetryProperties(attempts = Int.MAX_VALUE, delay = Duration.ofMillis(1000)),
                meterRegistry = meterRegistry,
            )
        }
        return ConsumerWorkerHolder(workers)
    }

    private fun consumerGroup(suffix: String): String {
        return "$env.protocol.union.search.$suffix"
    }

    private fun worker(suffix: String, index: Int): String {
        return "$env.$suffix.worker-$index"
    }
}
