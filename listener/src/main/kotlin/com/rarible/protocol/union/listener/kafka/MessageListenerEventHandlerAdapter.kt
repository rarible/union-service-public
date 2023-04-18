package com.rarible.protocol.union.listener.kafka

import com.rarible.protocol.union.core.handler.InternalEventHandler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.BatchMessageListener

class MessageListenerEventHandlerAdapter<T>(
    private val handler: InternalEventHandler<T>
) : BatchMessageListener<String, T> {
    override fun onMessage(records: List<ConsumerRecord<String, T>>) = runBlocking<Unit>(NonCancellable) {
        val recordsByKey = records.groupBy { it.key() }
        recordsByKey.values.map { group ->
            async {
                group.forEach {
                    try {
                        handler.handle(it.value())
                    } catch (e: Exception) {
                        logger.error("Unexpected exception during handling internal event $it", e)
                    }
                }
            }
        }.awaitAll()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MessageListenerEventHandlerAdapter::class.java)
    }
}