package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.protocol.union.enrichment.configuration.CustomCollectionMapping
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCollectionProperties
import org.springframework.stereotype.Component

@Component
class CustomCollectionItemFetcherFactory(
    private val customCollectionItemProvider: CustomCollectionItemProvider,
    enrichmentCollectionProperties: EnrichmentCollectionProperties
) {

    private val fetchersByCustomCollection = enrichmentCollectionProperties.mappings
        .associateBy { it.customCollection }
        .mapValues { createFetchers(it.value) }

    fun get(collectionId: String): List<CustomCollectionItemFetcher> {
        return fetchersByCustomCollection[collectionId] ?: emptyList()
    }

    private fun createFetchers(mapping: CustomCollectionMapping): List<CustomCollectionItemFetcher> {
        val result = ArrayList<CustomCollectionItemFetcher>(2)

        val itemIds = mapping.getItemIds().map { it.toDto() }
        if (itemIds.isNotEmpty()) {
            result.add(CustomCollectionItemFetcherByList(customCollectionItemProvider, itemIds))
        }

        val collectionIds = mapping.getCollectionIds().map { it.toDto() }
        if (collectionIds.isNotEmpty()) {
            result.add(CustomCollectionItemFetcherByCollection(customCollectionItemProvider, collectionIds))
        }

        val ranges = mapping.getRanges()
        if (ranges.isNotEmpty()) {
            result.add(CustomCollectionItemFetcherByRange(customCollectionItemProvider, ranges))
        }
        return result
    }

}