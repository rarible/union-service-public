package com.rarible.protocol.union.listener.kafka

import com.rarible.protocol.union.core.handler.InternalEventHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.lang.RuntimeException

@ExtendWith(MockKExtension::class)
internal class MessageListenerEventHandlerAdapterTest {
    @InjectMockKs
    private lateinit var messageListenerEventHandlerAdapter: MessageListenerEventHandlerAdapter<String>

    @MockK
    private lateinit var handler: InternalEventHandler<String>

    @Test
    fun handleBatch() = runBlocking<Unit> {
        val batch = listOf(
            ConsumerRecord("topic", 0, 0, "key1", "value1"),
            ConsumerRecord("topic", 0, 1, "key2", "value2"),
            ConsumerRecord("topic", 0, 3, "key3", "value3"),
        )
        coEvery { handler.handle("value2") } throws RuntimeException("")
        coEvery { handler.handle("value1") } returns Unit
        coEvery { handler.handle("value3") } returns Unit

        messageListenerEventHandlerAdapter.onMessage(batch)

        coVerify {
            handler.handle("value1")
            handler.handle("value2")
            handler.handle("value3")
        }
    }
}