package com.rarible.protocol.union.core.event

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.BlockchainEventHandlerWrapper
import com.rarible.protocol.union.core.handler.ConsumerWorkerGroup
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ConsumerFactory(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val meterRegistry: MeterRegistry
) {

    private val env = applicationEnvironmentInfo.name

    val itemGroup = consumerGroup(EventType.ITEM)
    val itemMetaGroup = consumerGroup(EventType.ITEM_META)
    val ownershipGroup = consumerGroup(EventType.OWNERSHIP)
    val collectionGroup = consumerGroup(EventType.COLLECTION)
    val orderGroup = consumerGroup(EventType.ORDER)
    val auctionGroup = consumerGroup(EventType.AUCTION)
    val activityGroup = consumerGroup(EventType.ACTIVITY)

    val unionSubscribeItemGroup = subscribeConsumerGroup(EventType.ITEM)
    val unionSubscribeOwnershipGroup = subscribeConsumerGroup(EventType.OWNERSHIP)
    val unionSubscribeOrderGroup = subscribeConsumerGroup(EventType.ORDER)

    fun <T> createItemConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(
            consumer, handler, daemon, workers, EventType.ITEM, batchSize
        )
    }

    fun <T> createItemMetaConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(
            consumer, handler, daemon, workers, EventType.ITEM_META, 1
        )
    }

    fun <T> createOwnershipConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(
            consumer, handler, daemon, workers, EventType.OWNERSHIP, batchSize
        )
    }

    fun <T> createOrderConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(
            consumer, handler, daemon, workers, EventType.ORDER, batchSize
        )
    }

    fun <T> createAuctionConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(
            consumer, handler, daemon, workers, EventType.AUCTION, batchSize
        )
    }

    fun <T> createActivityConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(
            consumer, handler, daemon, workers, EventType.ACTIVITY, batchSize
        )
    }

    fun <T> createCollectionConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(
            consumer, handler, daemon, workers, EventType.COLLECTION, batchSize
        )
    }

    private fun <T> createBlockchainConsumerWorkerGroup(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        eventType: EventType,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        val blockchain = handler.blockchain
        val workerCount = workers.getOrDefault(eventType.value, 1)
        val workerSet = (1..workerCount).map {
            val workerName = "${blockchain.name.lowercase()}-${eventType.value}-$it"
            val retryProperties = RetryProperties(attempts = Integer.MAX_VALUE, delay = Duration.ofSeconds(1))
            if (batchSize == 1) {
                ConsumerWorker(
                    consumer = consumer,
                    properties = daemonWorkerProperties,
                    eventHandler = BlockchainEventHandlerWrapper(handler),
                    meterRegistry = meterRegistry,
                    workerName = workerName,
                    retryProperties = retryProperties
                )
            } else {
                ConsumerBatchWorker(
                    consumer = consumer,
                    properties = daemonWorkerProperties.copy(consumerBatchSize = batchSize),
                    eventHandler = BlockchainEventHandlerWrapper(handler),
                    meterRegistry = meterRegistry,
                    workerName = workerName,
                    retryProperties = retryProperties
                )
            }
        }
        return ConsumerWorkerGroup(workerSet)
    }

    fun createUnionItemConsumerWorkerGroup(
        consumer: RaribleKafkaConsumer<ItemEventDto>,
        handler: ConsumerEventHandler<ItemEventDto>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        type: String
    ): ConsumerWorkerGroup<ItemEventDto> {
        return createUnionConsumerWorkerGroup(
            consumer, handler, daemonWorkerProperties, workers, type, EventType.ITEM
        )
    }

    fun createUnionOrderConsumerWorkerGroup(
        consumer: RaribleKafkaConsumer<OrderEventDto>,
        handler: ConsumerEventHandler<OrderEventDto>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        type: String
    ): ConsumerWorkerGroup<OrderEventDto> {
        return createUnionConsumerWorkerGroup(
            consumer, handler, daemonWorkerProperties, workers, type, EventType.ORDER
        )
    }

    fun createUnionOwnershipConsumerWorkerGroup(
        consumer: RaribleKafkaConsumer<OwnershipEventDto>,
        handler: ConsumerEventHandler<OwnershipEventDto>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        type: String
    ): ConsumerWorkerGroup<OwnershipEventDto> {
        return createUnionConsumerWorkerGroup(
            consumer, handler, daemonWorkerProperties, workers, type, EventType.OWNERSHIP
        )
    }

    private fun <T> createUnionConsumerWorkerGroup(
        consumer: RaribleKafkaConsumer<T>,
        handler: ConsumerEventHandler<T>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        type: String,
        eventType: EventType
    ): ConsumerWorkerGroup<T> {
        val workerCount = workers.getOrDefault(eventType.value, 1)
        val workerSet = (1..workerCount).map {
            ConsumerWorker(
                consumer = consumer,
                properties = daemonWorkerProperties,
                eventHandler = handler,
                meterRegistry = meterRegistry,
                workerName = "union-$type-${eventType.value}-$it"
            )
        }
        return ConsumerWorkerGroup(workerSet)
    }

    private fun consumerGroup(type: EventType): String {
        return "${env}.protocol.union.${type.value}"
    }

    private fun subscribeConsumerGroup(type: EventType): String {
        return "${env}.protocol.union.subscribe.${type.value}"
    }
}
