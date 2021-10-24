package com.rarible.protocol.union.core.handler

interface KafkaConsumerWorker<T> : AutoCloseable {

    fun start()

}