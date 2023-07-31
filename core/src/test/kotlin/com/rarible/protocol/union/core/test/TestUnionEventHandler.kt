package com.rarible.protocol.union.core.test

import com.rarible.core.kafka.RaribleKafkaEventHandler
import java.util.concurrent.ConcurrentLinkedDeque

class TestUnionEventHandler<T> : RaribleKafkaEventHandler<T> {

    val events = ConcurrentLinkedDeque<T>()

    override suspend fun handle(event: T) {
        events.add(event)
    }
}
