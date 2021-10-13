package com.rarible.protocol.union.core.continuation

interface ContinuationFactory<T, C : Continuation<C>> {

    fun getContinuation(entity: T): C

}