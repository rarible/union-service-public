package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.core.model.UnionOrder

interface BestOrderProvider<E> {

    val entityId: String
    val entityType: Class<E>

    suspend fun fetch(currencyId: String): UnionOrder?
}
