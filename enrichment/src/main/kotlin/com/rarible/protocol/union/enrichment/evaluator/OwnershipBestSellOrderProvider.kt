package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService

class OwnershipBestSellOrderProvider(
    private val ownershipId: ShortOwnershipId,
    private val currencyId: String,
    private val enrichmentOrderService: EnrichmentOrderService
) : BestOrderProvider<ShortOwnership> {

    override val entityId: String = ownershipId.toString()
    override val entityType: Class<ShortOwnership> get() = ShortOwnership::class.java

    override suspend fun fetch(): OrderDto? {
        return enrichmentOrderService.getBestSell(ownershipId, currencyId)
    }
}
