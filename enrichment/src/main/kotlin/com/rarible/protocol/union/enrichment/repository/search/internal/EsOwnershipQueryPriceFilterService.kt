package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.model.CurrencyRate
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemGenericFilter
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.EsOwnershipsSearchFilter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.stereotype.Service

@Service
class EsOwnershipQueryPriceFilterService(
    private val currencyService: CurrencyService,
    private val esEntityQueryPriceFilterService: EsEntityQueryPriceFilterService,
) {
    companion object {
        val logger by Logger()
    }

    suspend fun applyPriceFilter(
        query: BoolQueryBuilder,
        filter: EsOwnershipsSearchFilter,
        blockchains: Set<BlockchainDto>
    ) {
        if (filter.sellPriceFrom == null && filter.sellPriceTo == null) return

        val currencyRates = currencyService.getAllCurrencyRates()
            .filter { it.blockchain in blockchains }

        esEntityQueryPriceFilterService.applyPriceFilter(
            query,
            EsOwnership::bestSellCurrency.name,
            EsOwnership::bestSellAmount.name,
            filter.sellPriceCurrency,
            filter.sellPriceFrom,
            filter.sellPriceTo,
            currencyRates,
        )
    }
}
