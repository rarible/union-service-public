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
    val meta: EnrichmentMetaProperties = EnrichmentMetaProperties()
)

data class EnrichmentMetaProperties(
    val item: EnrichmentItemMetaProperties = EnrichmentItemMetaProperties()
    // TODO add for collections if needed
)

class EnrichmentItemMetaProperties(
    val customizers: EnrichmentItemMetaCustomizerProperties = EnrichmentItemMetaCustomizerProperties()
)

class EnrichmentItemMetaCustomizerProperties(
    val mattel: EnrichmentMattelMetaCustomizerProperties = EnrichmentMattelMetaCustomizerProperties()
)

class EnrichmentMattelMetaCustomizerProperties(
    val barbieCard: List<String> = emptyList(),
    val barbieToken: List<String> = emptyList(),
    val barbiePack: List<String> = emptyList(),
    val hwCard: List<String> = emptyList(),
    val hwPack: List<String> = emptyList(),
)

data class EnrichmentCurrenciesProperties(
    val bestBidByCurrencyWhitelist: List<String> = emptyList(),
)

data class EnrichmentCollectionProperties(
    val mappings: List<CustomCollectionMapping> = emptyList(),
)

// Contains fully-qualified collection/item identifiers
data class CustomCollectionMapping(
    val enabled: Boolean = true,
    // Full ID of collection for default mapping OR name of custom mapper
    val name: String,
    private val items: List<String> = emptyList(),
    private val collections: List<String> = emptyList(),
    private val ranges: List<String> = emptyList(),
    val meta: CustomCollectionMetaMapping = CustomCollectionMetaMapping()
) {

    fun getItemIds(): List<ShortItemId> = items.map { ShortItemId.of(it) }
    fun getCollectionIds(): List<EnrichmentCollectionId> = collections.map { EnrichmentCollectionId.of(it) }
    fun getRanges(): List<TokenRange> = ranges.map { TokenRange.of(it) }
}

data class CustomCollectionMetaMapping(
    private val collections: List<String> = emptyList(),
    val attributes: List<CustomCollectionMetaAttributeMapping> = emptyList()
) {

    fun getCollectionIds(): List<EnrichmentCollectionId> = collections.map { EnrichmentCollectionId.of(it) }
    fun getAttributes(): Map<String, Set<String>> = attributes.associateBy({ it.name }, { it.values })
}

data class CustomCollectionMetaAttributeMapping(
    val name: String,
    val values: Set<String>
)
