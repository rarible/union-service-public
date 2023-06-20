package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCurrenciesProperties
import com.rarible.protocol.union.enrichment.evaluator.BestBidOrderOwner
import com.rarible.protocol.union.enrichment.model.OriginOrdersOwner
import com.rarible.protocol.union.enrichment.model.ShortOrder
import org.springframework.stereotype.Service

@Service
class EnrichmentHelperService(
    private val featureFlagsProperties: FeatureFlagsProperties,
    currencyProperties: EnrichmentCurrenciesProperties,
) {

    private val bestBidByCurrencyWhitelist = HashSet(
        currencyProperties.bestBidByCurrencyWhitelist.map { IdParser.parseContract(it) }
    )

    fun <T> getExistingOrders(
        ordersOwners: Collection<T>
    ): List<ShortOrder> where T : BestBidOrderOwner<T>, T : OriginOrdersOwner {
        val result = ArrayList<ShortOrder>(ordersOwners.size) // Basic approximation - same size
        ordersOwners.forEach { result.addAll(getExistingOrders(it)) }
        return result
    }

    fun <T> getExistingOrders(ordersOwner: T?): List<ShortOrder> where T : BestBidOrderOwner<T>, T : OriginOrdersOwner {
        val bestOrders = ordersOwner?.getAllBestOrders() ?: emptyList()
        if (!featureFlagsProperties.enableItemBestBidsByCurrency) {
            return bestOrders
        }
        val bestBidOrders = ordersOwner?.bestBidOrders?.entries
        if (bestBidOrders.isNullOrEmpty()) {
            return bestOrders
        }

        val bestBidsByCurrency = bestBidOrders
            .filter { ContractAddress(it.value.blockchain, it.key) in bestBidByCurrencyWhitelist }
            .map { it.value }
            .ifEmpty { return bestOrders }

        return (bestOrders + bestBidsByCurrency).toSet().toList()
    }
}