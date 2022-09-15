package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.model.CurrencyRate
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemGenericFilter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.stereotype.Service

@Service
class EsEntityQueryPriceFilterService {
    companion object {
        val logger by Logger()
    }

    /**
     * Applies filter by price to the query
     */
    fun applyPriceFilter(
        query: BoolQueryBuilder,
        currencyFieldName: String,
        priceFieldName: String,
        priceCurrency: String?,
        priceFrom: Double?,
        priceTo: Double?,
        currencyRates: List<CurrencyRate>
    ) {
        val usdPriceFrom = calcPrice(priceCurrency, priceFrom, currencyRates)
        val usdPriceTo = calcPrice(priceCurrency, priceTo, currencyRates)

        query.applyMultiPriceFilter(
            currencyRates,
            priceFieldName,
            currencyFieldName,
            usdPriceFrom,
            usdPriceTo,
        )
    }

    private fun calcPrice(currency: String?, price: Double?, currencyRates: List<CurrencyRate>): Double? {
        if (price == null) return null
        // assume currency is USD
        if (currency == null) return price

        val currencyRate = currencyRates.find { it.currencyId == currency }
        return if (currencyRate == null) {
            logger.error("Couldn't find currency rate for $currency")
            null
        } else {
            price * currencyRate.rate.toDouble()
        }
    }

    private fun BoolQueryBuilder.applyMultiPriceFilter(
        currencyRates: List<CurrencyRate>,
        priceFieldName: String,
        currencyFieldName: String,
        minPriceUsd: Double?,
        maxPriceUsd: Double?,
    ) {
        if (minPriceUsd == null && maxPriceUsd == null) return
        val currenciesQuery = QueryBuilders.boolQuery()
        currencyRates.forEach { currencyRate ->
            currenciesQuery.applyCurrencyPriceFilter(
                priceFieldName = priceFieldName,
                currencyFieldName = currencyFieldName,
                currencyRate = currencyRate.rate.toDouble(),
                address = currencyRate.currencyId,
                minPriceUsd = minPriceUsd,
                maxPriceUsd = maxPriceUsd,
            )
        }
        must(currenciesQuery)
    }

    private fun BoolQueryBuilder.applyCurrencyPriceFilter(
        priceFieldName: String,
        currencyFieldName: String,
        currencyRate: Double,
        address: String,
        minPriceUsd: Double?,
        maxPriceUsd: Double?,
    ) {
        val priceQueryBuilder = QueryBuilders.rangeQuery(priceFieldName)
        if (minPriceUsd != null) {
            priceQueryBuilder.gte(minPriceUsd / currencyRate)
        }
        if (maxPriceUsd != null) {
            priceQueryBuilder.lte(maxPriceUsd / currencyRate)
        }
        should(
            QueryBuilders.boolQuery()
                .must(priceQueryBuilder)
                .must(QueryBuilders.termsQuery(currencyFieldName, address))
        )
    }
}
