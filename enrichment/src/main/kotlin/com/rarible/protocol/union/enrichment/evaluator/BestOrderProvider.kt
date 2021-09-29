package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.OrderDto

interface BestOrderProvider<E> {

    val entityId: String
    val entityType: Class<E>

    suspend fun fetch(): OrderDto?

}