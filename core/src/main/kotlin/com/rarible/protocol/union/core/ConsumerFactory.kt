package com.rarible.protocol.union.core

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

    companion object {

        const val ORDER = "order"
        const val AUCTION = "auction"
        const val ACTIVITY = "activity"
        const val ITEM = "item"
        const val ITEM_META = "itemMeta"
        const val COLLECTION = "collection"
        const val OWNERSHIP = "ownership"
    }

    private val env = applicationEnvironmentInfo.name

    //---------------- Blockchain handlers (external) ---------------//

    val itemGroup = consumerGroup(ITEM)
    val itemMetaGroup = consumerGroup(ITEM_META)
    val ownershipGroup = consumerGroup(OWNERSHIP)
    val collectionGroup = consumerGroup(COLLECTION)
    val orderGroup = consumerGroup(ORDER)
    val auctionGroup = consumerGroup(AUCTION)
    val activityGroup = consumerGroup(ACTIVITY)
    val unionSubscribeItemGroup = subscribeConsumerGroup(ITEM)
    val unionSubscribeOwnershipGroup = subscribeConsumerGroup(OWNERSHIP)
    val unionSubscribeOrderGroup = subscribeConsumerGroup(ORDER)

    fun <T> createItemConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(consumer, handler, daemon, workers, ITEM, batchSize)
    }

    fun <T> createItemMetaConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(consumer, handler, daemon, workers, ITEM_META, 1)
    }

    fun <T> createOwnershipConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(consumer, handler, daemon, workers, OWNERSHIP, batchSize)
    }

    fun <T> createOrderConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(consumer, handler, daemon, workers, ORDER, batchSize)
    }

    fun <T> createAuctionConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(consumer, handler, daemon, workers, AUCTION, batchSize)
    }

    fun <T> createActivityConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(consumer, handler, daemon, workers, ACTIVITY, batchSize)
    }

    fun <T> createCollectionConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        return createBlockchainConsumerWorkerGroup(consumer, handler, daemon, workers, COLLECTION, batchSize)
    }

    private fun <T> createBlockchainConsumerWorkerGroup(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        entityType: String,
        batchSize: Int
    ): ConsumerWorkerGroup<T> {
        val blockchain = handler.blockchain
        val workerCount = workers.getOrDefault(entityType, 1)
        val workerSet = (1..workerCount).map {
            if (batchSize == 1) {
                ConsumerWorker(
                    consumer = consumer,
                    properties = daemonWorkerProperties,
                    eventHandler = BlockchainEventHandlerWrapper(handler),
                    meterRegistry = meterRegistry,
                    workerName = "${blockchain.name.lowercase()}-${entityType}-$it",
                    retryProperties = RetryProperties(attempts = Integer.MAX_VALUE, delay = Duration.ofSeconds(1))
                )
            } else {
                ConsumerBatchWorker(
                    consumer = consumer,
                    properties = daemonWorkerProperties.copy(consumerBatchSize = batchSize),
                    eventHandler = BlockchainEventHandlerWrapper(handler),
                    meterRegistry = meterRegistry,
                    workerName = "${blockchain.name.lowercase()}-${entityType}-$it",
                    retryProperties = RetryProperties(attempts = Integer.MAX_VALUE, delay = Duration.ofSeconds(1))

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
        return createUnionConsumerWorkerGroup(consumer, handler, daemonWorkerProperties, workers, type, ITEM)
    }

    fun createUnionOrderConsumerWorkerGroup(
        consumer: RaribleKafkaConsumer<OrderEventDto>,
        handler: ConsumerEventHandler<OrderEventDto>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        type: String
    ): ConsumerWorkerGroup<OrderEventDto> {
        return createUnionConsumerWorkerGroup(consumer, handler, daemonWorkerProperties, workers, type, ORDER)
    }

    fun createUnionOwnershipConsumerWorkerGroup(
        consumer: RaribleKafkaConsumer<OwnershipEventDto>,
        handler: ConsumerEventHandler<OwnershipEventDto>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        type: String
    ): ConsumerWorkerGroup<OwnershipEventDto> {
        return createUnionConsumerWorkerGroup(consumer, handler, daemonWorkerProperties, workers, type, OWNERSHIP)
    }

    private fun <T> createUnionConsumerWorkerGroup(
        consumer: RaribleKafkaConsumer<T>,
        handler: ConsumerEventHandler<T>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        type: String,
        entityType: String
    ): ConsumerWorkerGroup<T> {
        val workerCount = workers.getOrDefault(entityType, 1)
        val workerSet = (1..workerCount).map {
            ConsumerWorker(
                consumer = consumer,
                properties = daemonWorkerProperties,
                eventHandler = handler,
                meterRegistry = meterRegistry,
                workerName = "union-$type-${entityType}-$it"
            )
        }
        return ConsumerWorkerGroup(workerSet)
    }

    private fun consumerGroup(suffix: String): String {
        return "${env}.protocol.union.${suffix}"
    }

    private fun subscribeConsumerGroup(suffix: String): String {
        return "${env}.protocol.union.subscribe.${suffix}"
    }
}
