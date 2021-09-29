package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.OrderService

class OwnershipBestSellOrderProvider(
    private val ownershipId: ShortOwnershipId,
    private val orderService: OrderService
) : BestOrderProvider<ShortOwnership> {

    override val entityId: String = ownershipId.toString()
    override val entityType: Class<ShortOwnership> get() = ShortOwnership::class.java

    override suspend fun fetch(): OrderDto? {
        return orderService.getBestSell(ownershipId)
    }
}