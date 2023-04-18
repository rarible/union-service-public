package com.rarible.protocol.union.listener.kafka

import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.listener.kafka.MessageListenerEventHandlerAdapter
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class MessageListenerEventHandlerAdapterTest {
    @InjectMockKs
    private lateinit var messageListenerEventHandlerAdapter: MessageListenerEventHandlerAdapter<String>

    @MockK
    private lateinit var handler: InternalEventHandler<String>

    @Test
    fun handleBatch() = runBlocking<Unit> {
        val batch = listOf(
            ConsumerRecord("topic", 0, 0, "key", "value1"),
            ConsumerRecord("topic", 0, 1, "key2", "value2"),
            ConsumerRecord("topic", 0, 3, "key3", "value3"),
        )
        coEvery { handler.handle(any()) } returns Unit

        messageListenerEventHandlerAdapter.onMessage(batch)

        coVerify(ordering = Ordering.SEQUENCE) {
            handler.handle("value1")
            handler.handle("value2")
            handler.handle("value3")
        }
    }
}