package com.rarible.protocol.union.listener.handler

import com.rarible.core.daemon.sequential.ConsumerWorker

class SingleKafkaConsumerWorker<T>(
    private val worker: ConsumerWorker<T>
) : KafkaConsumerWorker {
    override fun start() {
        worker.start()
    }

    override fun close() {
        worker.close()
    }
}