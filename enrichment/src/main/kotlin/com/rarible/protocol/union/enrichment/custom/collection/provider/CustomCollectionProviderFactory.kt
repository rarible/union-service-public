package com.rarible.protocol.union.enrichment.custom.collection.provider

import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.configuration.CustomCollectionMapping
import org.springframework.stereotype.Component

@Component
class CustomCollectionProviderFactory(
    private val artBlocksCustomCollectionProvider: ArtBlocksCustomCollectionProvider
) {

    fun create(mapping: CustomCollectionMapping): CustomCollectionProvider {
        return when (mapping.name) {
            ArtBlocksCustomCollectionProvider.MAPPING_NAME -> artBlocksCustomCollectionProvider
            else -> DefaultCustomCollectionProvider(IdParser.parseCollectionId(mapping.name))
        }
    }
}
