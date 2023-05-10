package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.enrichment.configuration.EnrichmentProperties
import com.rarible.protocol.union.enrichment.evaluator.BestBidOrderOwner
import com.rarible.protocol.union.enrichment.evaluator.BlockchainAware
import com.rarible.protocol.union.enrichment.model.OriginOrdersOwner
import com.rarible.protocol.union.enrichment.model.ShortOrder
import org.springframework.stereotype.Service

@Service
class EnrichmentHelperService(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val enrichmentProperties: EnrichmentProperties,
) {

    fun <T> getExistingOrders(ordersOwner: T?): List<ShortOrder>
        where
        T : BestBidOrderOwner<T>,
        T : OriginOrdersOwner,
        T : BlockchainAware =
        if (featureFlagsProperties.enableItemBestBidsByCurrency) {
            val bestOrders = ordersOwner?.getAllBestOrders() ?: emptyList()
            val bestBidsByCurrency = ordersOwner?.bestBidOrders?.entries
                ?.filter {
                    "${ordersOwner.blockchain}:${it.key}" in enrichmentProperties.currencies.bestBidByCurrencyWhitelist
                }
                ?.map { it.value } ?: emptyList()
            (bestOrders + bestBidsByCurrency).toSet().toList()
        } else {
            ordersOwner?.getAllBestOrders()
        } ?: emptyList()
}