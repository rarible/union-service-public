package com.rarible.protocol.union.listener.config

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.protocol.union.core.handler.BatchedConsumerWorker
import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.handler.InternalEventHandlerWrapper
import com.rarible.protocol.union.dto.BlockchainDto
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class InternalConsumerFactory(
    private val meterRegistry: MeterRegistry
) {

    companion object {

        const val RECONCILIATION = "reconciliation"
        const val DEFAULT_BLOCKCHAIN_WORKER_COUNT = 16
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    fun <T> createReconciliationMarkEventConsumer(
        consumer: (i: Int) -> RaribleKafkaConsumer<T>,
        handler: InternalEventHandler<T>,
        daemon: DaemonWorkerProperties,
        workers: Int
    ): BatchedConsumerWorker<T> {
        logger.info("Creating {} reconciliation mark consumers", workers)
        return createInternalBatchedConsumerWorker(consumer, handler, daemon, workers, RECONCILIATION)
    }

    fun <T> createInternalBlockchainEventConsumer(
        consumer: (i: Int) -> RaribleKafkaConsumer<T>,
        handler: InternalEventHandler<T>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>,
        blockchain: BlockchainDto
    ): BatchedConsumerWorker<T> {
        val type = blockchain.name.lowercase()
        val workerCount = workers[type] ?: DEFAULT_BLOCKCHAIN_WORKER_COUNT

        logger.info("Creating internal {} consumers for blockchain {}", workerCount, blockchain)
        return createInternalBatchedConsumerWorker(consumer, handler, daemon, workerCount, type)
    }

    @Deprecated("Should be replaced by createInternalBlockchainEventConsumer()")
    fun <T> createWrappedEventConsumer(
        consumer: (i: Int) -> RaribleKafkaConsumer<T>,
        handler: InternalEventHandler<T>,
        daemon: DaemonWorkerProperties,
        workers: Int
    ): BatchedConsumerWorker<T> {
        logger.info("Creating {} wrapped event consumers (SHOULD BE REMOVED)", workers)
        return createInternalBatchedConsumerWorker(consumer, handler, daemon, workers, "wrapped")
    }

    fun <T> createInternalBatchedConsumerWorker(
        consumer: (i: Int) -> RaribleKafkaConsumer<T>,
        handler: InternalEventHandler<T>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Int,
        type: String
    ): BatchedConsumerWorker<T> {
        val workerSet = (1..workers).map {
            ConsumerWorker(
                consumer = consumer(it),
                properties = daemonWorkerProperties,
                eventHandler = InternalEventHandlerWrapper(handler),
                meterRegistry = meterRegistry,
                workerName = "internal-${type}-$it"
            )
        }
        return BatchedConsumerWorker(workerSet)
    }
}
