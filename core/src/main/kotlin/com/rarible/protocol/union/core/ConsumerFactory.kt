package com.rarible.protocol.union.core

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.protocol.union.core.handler.BatchedConsumerWorker
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.BlockchainEventHandlerWrapper
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

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
        const val COLLECTION = "collection"
        const val OWNERSHIP = "ownership"
    }

    private val env = applicationEnvironmentInfo.name

    //---------------- Blockchain handlers (external) ---------------//

    val itemGroup = consumerGroup(ITEM)
    val ownershipGroup = consumerGroup(OWNERSHIP)
    val collectionGroup = consumerGroup(COLLECTION)
    val orderGroup = consumerGroup(ORDER)
    val auctionGroup = consumerGroup(AUCTION)
    val activityGroup = consumerGroup(ACTIVITY)
    val unionSubscribeItemGroup = subscribeConsumerGroup(ITEM)
    val unionSubscribeOwnershipGroup = subscribeConsumerGroup(OWNERSHIP)

    fun <T> createItemConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>
    ): BatchedConsumerWorker<T> {
        return createBlockchainBatchedConsumerWorker(consumer, handler, daemon, workers, ITEM)
    }

    fun <T> createOwnershipConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>
    ): BatchedConsumerWorker<T> {
        return createBlockchainBatchedConsumerWorker(consumer, handler, daemon, workers, OWNERSHIP)
    }

    fun <T> createOrderConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>
    ): BatchedConsumerWorker<T> {
        return createBlockchainBatchedConsumerWorker(consumer, handler, daemon, workers, ORDER)
    }

    fun <T> createAuctionConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>
    ): BatchedConsumerWorker<T> {
        return createBlockchainBatchedConsumerWorker(consumer, handler, daemon, workers, AUCTION)
    }

    fun <T> createActivityConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>
    ): BatchedConsumerWorker<T> {
        return createBlockchainBatchedConsumerWorker(consumer, handler, daemon, workers, ACTIVITY)
    }

    fun <T> createCollectionConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>
    ): BatchedConsumerWorker<T> {
        return createBlockchainBatchedConsumerWorker(consumer, handler, daemon, workers, COLLECTION)
    }

    fun <T> createBlockchainBatchedConsumerWorker(
        consumer: RaribleKafkaConsumer<T>,
        handler: BlockchainEventHandler<T, *>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        entityType: String
    ): BatchedConsumerWorker<T> {
        val blockchain = handler.blockchain
        val workerCount = workers.getOrDefault(entityType, 1)
        val workerSet = (1..workerCount).map {
            ConsumerWorker(
                consumer = consumer,
                properties = daemonWorkerProperties,
                eventHandler = BlockchainEventHandlerWrapper(handler),
                meterRegistry = meterRegistry,
                workerName = "${blockchain.name.lowercase()}-${entityType}-$it"
            )
        }
        return BatchedConsumerWorker(workerSet)
    }

    fun createUnionItemBatchedConsumerWorker(
        consumer: RaribleKafkaConsumer<ItemEventDto>,
        handler: ConsumerEventHandler<ItemEventDto>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        type: String
    ): BatchedConsumerWorker<ItemEventDto> {
        return createUnionBatchedConsumerWorker(consumer, handler, daemonWorkerProperties, workers, type, ITEM)
    }

    fun createUnionOwnershipBatchedConsumerWorker(
        consumer: RaribleKafkaConsumer<OwnershipEventDto>,
        handler: ConsumerEventHandler<OwnershipEventDto>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        type: String
    ): BatchedConsumerWorker<OwnershipEventDto> {
        return createUnionBatchedConsumerWorker(consumer, handler, daemonWorkerProperties, workers, type, OWNERSHIP)
    }

    private fun <T> createUnionBatchedConsumerWorker(
        consumer: RaribleKafkaConsumer<T>,
        handler: ConsumerEventHandler<T>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        type: String,
        entityType: String
    ): BatchedConsumerWorker<T> {
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
        return BatchedConsumerWorker(workerSet)
    }

    private fun consumerGroup(suffix: String): String {
        return "${env}.protocol.union.${suffix}"
    }

    private fun subscribeConsumerGroup(suffix: String): String {
        return "${env}.protocol.union.subscribe.${suffix}"
    }
}
