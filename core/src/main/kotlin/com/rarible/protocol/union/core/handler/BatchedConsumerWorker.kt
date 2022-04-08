package com.rarible.protocol.union.core.handler

import com.rarible.core.daemon.sequential.ConsumerWorker

class BatchedConsumerWorker<T>(
    val workers: List<ConsumerWorker<T>>
) : KafkaConsumerWorker<T> {

    override fun start() {
        workers.forEach { it.start() }
    }

    override fun close() {
        workers.forEach { it.close() }
    }

}