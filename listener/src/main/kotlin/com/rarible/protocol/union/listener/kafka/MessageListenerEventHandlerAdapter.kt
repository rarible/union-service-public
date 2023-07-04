package com.rarible.protocol.union.listener.kafka

import com.rarible.core.logging.asyncWithTraceId
import com.rarible.core.logging.withBatchId
import com.rarible.core.logging.withTraceId
import com.rarible.protocol.union.core.handler.InternalEventHandler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.BatchMessageListener

class MessageListenerEventHandlerAdapter<T>(
    private val handler: InternalEventHandler<T>
) : BatchMessageListener<String, T> {

    override fun onMessage(records: List<ConsumerRecord<String, T>>) = runBlocking<Unit>(NonCancellable) {
        val now = System.currentTimeMillis()
        withBatchId {
            val recordsByKey = records.groupBy { it.key() }
            recordsByKey.values.map { group ->
                asyncWithTraceId(context = NonCancellable) {
                    withTraceId {
                        group.forEach {
                            handler.handle(it.value())
                        }
                    }
                }
            }.awaitAll()
        }
    }
}