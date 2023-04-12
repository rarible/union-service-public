package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ItemDtoConverterTest {

    @Test
    fun `convert - ok, custom collection`() {
        val unionItem = randomUnionItem(randomEthItemId())
        val customCollection = randomEthCollectionId()
        val enrichCustomCollectionId = EnrichmentCollectionId(customCollection)

        val result = ItemDtoConverter.convert(item = unionItem, customCollection = enrichCustomCollectionId)

        assertThat(result.collection).isEqualTo(customCollection)
    }

    @Test
    fun `convert - ok, custom collection not specified`() {
        val unionItem = randomUnionItem(randomEthItemId())

        val result = ItemDtoConverter.convert(item = unionItem)

        assertThat(result.collection).isEqualTo(unionItem.collection)
    }

}