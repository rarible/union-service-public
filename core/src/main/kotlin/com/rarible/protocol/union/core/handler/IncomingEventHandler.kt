package com.rarible.protocol.union.core.handler

interface IncomingEventHandler<T> {

    suspend fun onEvent(event: T)

    suspend fun onEvents(events: Collection<T>)
}
