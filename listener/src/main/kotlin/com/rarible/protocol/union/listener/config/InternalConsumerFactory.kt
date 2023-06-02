package com.rarible.protocol.union.listener.config

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.protocol.union.core.handler.ConsumerWorkerGroup
import com.rarible.protocol.union.core.handler.InternalBatchEventHandler
import com.rarible.protocol.union.core.handler.InternalBatchEventHandlerWrapper
import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.handler.InternalEventHandlerWrapper
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class InternalConsumerFactory(
    private val meterRegistry: MeterRegistry
) {

    companion object {

        const val RECONCILIATION = "reconciliation"
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    fun <T> createReconciliationMarkEventConsumer(
        consumer: (i: Int) -> RaribleKafkaConsumer<T>,
        handler: InternalEventHandler<T>,
        daemon: DaemonWorkerProperties,
        workers: Int
    ): ConsumerWorkerGroup<T> {
        logger.info("Creating {} reconciliation mark consumers", workers)
        return createInternalBatchedConsumerWorker(consumer, handler, daemon, workers, RECONCILIATION)
    }

    fun <T> createInternalBatchedConsumerWorker(
        consumer: (i: Int) -> RaribleKafkaConsumer<T>,
        handler: InternalEventHandler<T>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Int,
        type: String
    ): ConsumerWorkerGroup<T> {
        val workerSet = (1..workers).map {
            ConsumerWorker(
                consumer = consumer(it),
                properties = daemonWorkerProperties,
                eventHandler = InternalEventHandlerWrapper(handler),
                meterRegistry = meterRegistry,
                workerName = "internal-${type}-$it"
            )
        }
        return ConsumerWorkerGroup(workerSet)
    }

    fun <T> createInternalBatchedConsumerWorker(
        consumer: (i: Int) -> RaribleKafkaConsumer<T>,
        handler: InternalBatchEventHandler<T>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Int,
        type: String
    ): ConsumerWorkerGroup<T> {
        val workerSet = (1..workers).map {
            ConsumerBatchWorker(
                consumer = consumer(it),
                properties = daemonWorkerProperties,
                eventHandler = InternalBatchEventHandlerWrapper(handler),
                meterRegistry = meterRegistry,
                workerName = "internal-${type}-$it"
            )
        }
        return ConsumerWorkerGroup(workerSet)
    }
}
