package com.rarible.protocol.union.core.handler

interface InternalEventHandler<B> {

    suspend fun handle(event: B)

}