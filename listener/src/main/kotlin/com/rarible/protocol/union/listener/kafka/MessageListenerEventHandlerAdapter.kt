package com.rarible.protocol.union.listener.kafka

import com.rarible.protocol.union.core.handler.InternalEventHandler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.BatchMessageListener

class MessageListenerEventHandlerAdapter<T>(
    private val handler: InternalEventHandler<T>
) : BatchMessageListener<String, T> {
    override fun onMessage(records: List<ConsumerRecord<String, T>>) = runBlocking<Unit>(NonCancellable) {
        records.forEach {
            handler.handle(it.value())
        }
    }
}