package com.rarible.protocol.union.listener.handler

interface KafkaConsumerWorker : AutoCloseable {

    fun start()

}