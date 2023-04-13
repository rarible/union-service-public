package com.rarible.protocol.union.enrichment.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "enrichment")
class EnrichmentProperties(
    val collection: EnrichmentCollectionProperties = EnrichmentCollectionProperties()
)

data class EnrichmentCollectionProperties(
    val mappings: List<CustomCollectionMapping> = emptyList(),
)

// Contains fully-qualified collection/item identifiers
data class CustomCollectionMapping(
    val customCollection: String,
    val items: List<String> = emptyList(),
    val collections: List<String> = emptyList()
)