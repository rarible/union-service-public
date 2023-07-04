package com.rarible.protocol.union.listener.kafka

import com.rarible.core.logging.asyncWithTraceId
import com.rarible.core.logging.withBatchId
import com.rarible.core.logging.withTraceId
import com.rarible.protocol.union.core.handler.InternalEventHandler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.BatchMessageListener
import java.util.UUID

class MessageListenerEventHandlerAdapter<T>(
    private val handler: InternalEventHandler<T>
) : BatchMessageListener<String, T> {

    private val listenerId = UUID.randomUUID()

    override fun onMessage(records: List<ConsumerRecord<String, T>>) = runBlocking<Unit>(NonCancellable) {
        val now = System.currentTimeMillis()
        withBatchId {
            logger.info("Starting to handle batch of {} entities by listener {}", records.size, listenerId)
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
            logger.info("Finished to handle batch by {} ({}ms)", listenerId, System.currentTimeMillis() - now)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MessageListenerEventHandlerAdapter::class.java)
    }
}