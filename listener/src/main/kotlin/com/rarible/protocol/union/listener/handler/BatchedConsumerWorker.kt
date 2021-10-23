package com.rarible.protocol.union.listener.handler

import com.rarible.core.daemon.sequential.ConsumerWorker

class BatchedConsumerWorker<T>(
    private val enabled: Boolean,
    private val workers: List<ConsumerWorker<T>>
) : KafkaConsumerWorker<T> {

    override fun start() {
        if (enabled) {
            workers.forEach { it.start() }
        }
    }

    override fun close() {
        if (enabled) {
            workers.forEach { it.close() }
        }
    }

}