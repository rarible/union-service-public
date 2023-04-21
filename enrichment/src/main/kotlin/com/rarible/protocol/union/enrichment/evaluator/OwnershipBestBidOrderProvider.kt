package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService

class OwnershipBestBidOrderProvider(
    private val ownershipId: ShortOwnershipId,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val origin: String? = null
) : BestOrderProvider<ShortOwnership> {

    override val entityId: String = ownershipId.toString()
    override val entityType: Class<ShortOwnership> get() = ShortOwnership::class.java

    override suspend fun fetch(currencyId: String): UnionOrder? {
        return null // Bid order is not supported for ownership yet
    }

    class Factory(
        private val ownershipId: ShortOwnershipId,
        private val enrichmentOrderService: EnrichmentOrderService,
    ) : BestOrderProviderFactory<ShortOwnership> {

        override fun create(origin: String?): BestOrderProvider<ShortOwnership> {
            return OwnershipBestBidOrderProvider(ownershipId, enrichmentOrderService, origin)
        }

    }
}
