package com.rarible.protocol.union.core.handler

interface InternalBatchEventHandler<B> {

    suspend fun handle(events: List<B>)
}