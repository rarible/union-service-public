package com.rarible.protocol.union.core.event

interface OutgoingEventListener<T> {

    suspend fun onEvent(event: T)
}
