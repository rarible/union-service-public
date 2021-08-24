package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.KafkaConsumer
import com.rarible.ethereum.domain.Blockchain
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

class EthereumCompositeConsumerWorker<T>(
    private val consumerGroup: String,
    private val workerName: String,
    private val consumerFactory: ConsumerFactory<T>,
    private val eventHandler: ConsumerEventHandler<T>,
    private val properties: DaemonWorkerProperties = DaemonWorkerProperties(),
    private val retryProperties: RetryProperties = RetryProperties(),
    private val meterRegistry: MeterRegistry = SimpleMeterRegistry()
) : AutoCloseable {

    private val consumerWorkers = Blockchain.values().map { blockchain ->
        ConsumerWorker(
            consumer = consumerFactory.createEventsConsumer(consumerGroup, blockchain),
            properties = properties,
            eventHandler = eventHandler,
            retryProperties = retryProperties,
            meterRegistry = meterRegistry,
            workerName = blockchain.name +"_$workerName"
        )
    }

    fun start() {
        consumerWorkers.forEach { it.start() }
    }

    override fun close() {
        consumerWorkers.forEach { it.close() }
    }

    interface ConsumerFactory<T> {
        fun createEventsConsumer(group: String, blockchain: Blockchain): KafkaConsumer<T>

        companion object {
            fun <T> wrap(body: (group: String, blockchain: Blockchain) -> KafkaConsumer<T>): ConsumerFactory<T> {
                return object : ConsumerFactory<T> {
                    override fun createEventsConsumer(group: String, blockchain: Blockchain): KafkaConsumer<T> {
                        return body(group, blockchain)
                    }
                }
            }
        }
    }
}
