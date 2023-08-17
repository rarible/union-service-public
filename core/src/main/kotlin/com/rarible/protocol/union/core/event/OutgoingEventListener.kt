package com.rarible.protocol.union.core.event

interface OutgoingEventListener<T> {

    suspend fun onEvent(event: T)

//    suspend fun onEvents(events: List<T>)
}
