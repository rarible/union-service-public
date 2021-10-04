package com.rarible.protocol.union.listener.handler

import com.rarible.core.daemon.sequential.ConsumerWorker

class SingleConsumerWorker<T>(
    private val worker: ConsumerWorker<T>
) : KafkaConsumerWorker<T> {

    override fun start() {
        worker.start()
    }

    override fun close() {
        worker.close()
    }
}