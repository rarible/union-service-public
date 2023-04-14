package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.configuration.CustomCollectionMapping
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCollectionProperties
import org.springframework.stereotype.Component

@Component
class CustomCollectionItemFetcherProvider(
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

        val itemIds = mapping.items.map { IdParser.parseItemId(it) }
        if (itemIds.isNotEmpty()) {
            result.add(CustomCollectionItemFetcherByList(customCollectionItemProvider, itemIds))
        }

        val collectionIds = mapping.collections.map { IdParser.parseCollectionId(it) }
        if (collectionIds.isNotEmpty()) {
            result.add(CustomCollectionItemFetcherByCollection(customCollectionItemProvider, collectionIds))
        }
        return result
    }

}