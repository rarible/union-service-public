package com.rarible.protocol.union.listener.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.protocol.union.core.handler.BatchedConsumerWorker
import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.handler.InternalEventHandlerWrapper
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class InternalConsumerFactory(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val meterRegistry: MeterRegistry
) {

    companion object {

        const val WRAPPED = "wrapped"
        const val RECONCILIATION = "reconciliation"
    }

    fun <T> createReconciliationMarkEventConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: InternalEventHandler<T>,
        daemon: DaemonWorkerProperties,
        workerCount: Int
    ): BatchedConsumerWorker<T> {
        val workers = mapOf(RECONCILIATION to 1)
        return createInternalBatchedConsumerWorker(consumer, handler, daemon, workers, RECONCILIATION)
    }

    fun <T> createWrappedEventConsumer(
        consumer: RaribleKafkaConsumer<T>,
        handler: InternalEventHandler<T>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>
    ): BatchedConsumerWorker<T> {
        return createInternalBatchedConsumerWorker(consumer, handler, daemon, workers, WRAPPED)
    }

    fun <T> createInternalBatchedConsumerWorker(
        consumer: RaribleKafkaConsumer<T>,
        handler: InternalEventHandler<T>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        entityType: String
    ): BatchedConsumerWorker<T> {
        val workerCount = workers.getOrDefault(entityType, 1)
        val workerSet = (1..workerCount).map {
            ConsumerWorker(
                consumer = consumer,
                properties = daemonWorkerProperties,
                eventHandler = InternalEventHandlerWrapper(handler),
                meterRegistry = meterRegistry,
                workerName = "internal-${entityType}-$it"
            )
        }
        return BatchedConsumerWorker(workerSet)
    }
}
