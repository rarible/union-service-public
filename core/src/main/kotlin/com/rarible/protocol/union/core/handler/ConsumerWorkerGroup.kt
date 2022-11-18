package com.rarible.protocol.union.core.handler

import com.rarible.core.daemon.sequential.SequentialDaemonWorker

class ConsumerWorkerGroup<T>(
    val workers: List<SequentialDaemonWorker>
) : KafkaConsumerWorker<T> {

    override fun start() {
        workers.forEach { it.start() }
    }

    override fun close() {
        workers.forEach { it.close() }
    }

}