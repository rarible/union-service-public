package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsItemGenericFilter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import org.elasticsearch.index.query.BoolQueryBuilder
import org.springframework.stereotype.Service

@Service
class EsItemQueryPriceFilterService(
    private val currencyService: CurrencyService,
    private val esEntityQueryPriceFilterService: EsEntityQueryPriceFilterService,
) {

    suspend fun applyPriceFilter(
        query: BoolQueryBuilder,
        filter: EsItemGenericFilter,
        blockchains: Set<BlockchainDto>
    ) {
        val currencyRates = currencyService.getAllCurrencyRates()
            .filter { it.blockchain in blockchains }

        esEntityQueryPriceFilterService.applyPriceFilter(
            query,
            EsItem::bestSellCurrency.name,
            EsItem::bestSellAmount.name,
            filter.sellPriceCurrency,
            filter.sellPriceFrom,
            filter.sellPriceTo,
            currencyRates,
        )

        esEntityQueryPriceFilterService.applyPriceFilter(
            query,
            EsItem::bestBidCurrency.name,
            EsItem::bestBidAmount.name,
            filter.bidPriceCurrency,
            filter.bidPriceFrom,
            filter.bidPriceTo,
            currencyRates,
        )
    }
}
