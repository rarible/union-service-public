package com.rarible.protocol.union.listener.handler

interface KafkaConsumerWorker<T> : AutoCloseable {

    fun start()

}