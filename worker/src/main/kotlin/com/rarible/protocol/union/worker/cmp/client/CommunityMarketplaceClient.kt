package com.rarible.protocol.union.worker.cmp.client

import com.rarible.marketplace.generated.whitelabelinternal.api.client.MarketplacesControllerApi
import com.rarible.protocol.union.enrichment.converter.MarketplaceConverter
import com.rarible.protocol.union.enrichment.model.Marketplace
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component

@Component
class CommunityMarketplaceClient(
    private val marketplacesApi: MarketplacesControllerApi,
) {
    suspend fun getMarketplaces(fromId: String?): List<Marketplace> {
        return marketplacesApi.getCommunityMarketplaces(fromId, 20)
            .asFlow()
            .mapNotNull(MarketplaceConverter::convert)
            .toList()
    }
}
