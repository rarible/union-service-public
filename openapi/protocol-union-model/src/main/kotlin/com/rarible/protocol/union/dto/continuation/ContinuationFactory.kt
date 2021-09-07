package com.rarible.protocol.union.dto.continuation

interface ContinuationFactory<T, C : Continuation<C>> {

    fun getContinuation(entity: T): C

}