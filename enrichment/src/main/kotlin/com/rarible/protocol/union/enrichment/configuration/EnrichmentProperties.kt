package com.rarible.protocol.union.enrichment.configuration

import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.util.TokenRange
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "enrichment")
class EnrichmentProperties(
    val collection: EnrichmentCollectionProperties = EnrichmentCollectionProperties(),
    val currencies: EnrichmentCurrenciesProperties = EnrichmentCurrenciesProperties(),
)

data class EnrichmentCurrenciesProperties(
    val bestBidByCurrencyWhitelist: List<String> = emptyList(),
)

data class EnrichmentCollectionProperties(
    val mappings: List<CustomCollectionMapping> = emptyList(),
)

// Contains fully-qualified collection/item identifiers
data class CustomCollectionMapping(
    val customCollection: String,
    private val items: List<String> = emptyList(),
    private val collections: List<String> = emptyList(),
    private val ranges: List<String> = emptyList()
) {

    fun getItemIds(): List<ShortItemId> = items.map { ShortItemId.of(it) }
    fun getCollectionIds(): List<EnrichmentCollectionId> = collections.map { EnrichmentCollectionId.of(it) }
    fun getRanges(): List<TokenRange> = ranges.map { TokenRange.of(it) }

}
