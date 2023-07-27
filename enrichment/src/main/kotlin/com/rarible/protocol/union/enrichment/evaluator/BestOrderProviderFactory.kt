package com.rarible.protocol.union.enrichment.evaluator

interface BestOrderProviderFactory<T> {

    fun create(origin: String?): BestOrderProvider<T>
}
