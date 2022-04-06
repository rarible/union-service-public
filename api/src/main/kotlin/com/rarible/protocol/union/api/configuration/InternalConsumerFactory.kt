package com.rarible.protocol.union.api.configuration

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
    private val meterRegistry: MeterRegistry
) {

    companion object {
        const val WRAPPED = "wrapped"
    }

    fun <T> createWrappedEventConsumer(
        consumer: (i: Int) -> RaribleKafkaConsumer<T>,
        handler: InternalEventHandler<T>,
        daemon: DaemonWorkerProperties,
        workers: Map<String, Int>
    ): BatchedConsumerWorker<T> {
        return createInternalBatchedConsumerWorker(consumer, handler, daemon, workers, WRAPPED)
    }

    fun <T> createInternalBatchedConsumerWorker(
        consumer: (i: Int) -> RaribleKafkaConsumer<T>,
        handler: InternalEventHandler<T>,
        daemonWorkerProperties: DaemonWorkerProperties,
        workers: Map<String, Int>,
        entityType: String
    ): BatchedConsumerWorker<T> {
        val workerCount = workers.getOrDefault(entityType, 1)
        val workerSet = (1..workerCount).map {
            ConsumerWorker(
                consumer = consumer(it),
                properties = daemonWorkerProperties,
                eventHandler = InternalEventHandlerWrapper(handler),
                meterRegistry = meterRegistry,
                workerName = "internal-${entityType}-$it"
            )
        }
        return BatchedConsumerWorker(workerSet)
    }
}
