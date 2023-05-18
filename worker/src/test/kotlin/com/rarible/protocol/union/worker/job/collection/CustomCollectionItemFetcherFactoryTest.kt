package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.enrichment.configuration.CustomCollectionMapping
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCollectionProperties
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CustomCollectionItemFetcherFactoryTest {

    private val customCollectionItemProvider: CustomCollectionItemProvider = mockk()

    @Test
    fun `get - ok, only items`() {
        val customCollection = randomEthCollectionId()
        val itemId = randomEthItemId()

        val mapping = CustomCollectionMapping(
            customCollection = customCollection.fullId(),
            items = listOf(itemId.fullId())
        )

        val properties = EnrichmentCollectionProperties(listOf(mapping))

        val provider = CustomCollectionItemFetcherFactory(customCollectionItemProvider, properties)
        val fetchers = provider.get(customCollection.fullId())

        assertThat(fetchers).hasSize(1)
        assertThat(fetchers[0]).isInstanceOf(CustomCollectionItemFetcherByList::class.java)
    }

    @Test
    fun `get - ok, only collections`() {
        val customCollection = randomEthCollectionId()
        val collectionId = randomEthCollectionId()

        val mapping = CustomCollectionMapping(
            customCollection = customCollection.fullId(),
            collections = listOf(collectionId.fullId())
        )

        val properties = EnrichmentCollectionProperties(listOf(mapping))

        val provider = CustomCollectionItemFetcherFactory(customCollectionItemProvider, properties)
        val fetchers = provider.get(customCollection.fullId())

        assertThat(fetchers).hasSize(1)
        assertThat(fetchers[0]).isInstanceOf(CustomCollectionItemFetcherByCollection::class.java)
    }

    @Test
    fun `get - ok`() {
        val customCollection = randomEthCollectionId()
        val itemId = randomEthItemId()
        val collectionId = randomEthCollectionId()

        val mapping = CustomCollectionMapping(
            customCollection = customCollection.fullId(),
            items = listOf(itemId.fullId()),
            collections = listOf(collectionId.fullId())
        )

        val properties = EnrichmentCollectionProperties(listOf(mapping))

        val provider = CustomCollectionItemFetcherFactory(customCollectionItemProvider, properties)
        val fetchers = provider.get(customCollection.fullId())

        assertThat(fetchers).hasSize(2)
        assertThat(fetchers[0]).isInstanceOf(CustomCollectionItemFetcherByList::class.java)
        assertThat(fetchers[1]).isInstanceOf(CustomCollectionItemFetcherByCollection::class.java)
    }

    @Test
    fun `get - not found`() {
        val customCollection = randomEthCollectionId()

        val mapping = CustomCollectionMapping(
            customCollection = customCollection.fullId()
        )

        val properties = EnrichmentCollectionProperties(listOf(mapping))

        val provider = CustomCollectionItemFetcherFactory(customCollectionItemProvider, properties)
        val fetchers = provider.get(customCollection.fullId())

        assertThat(fetchers).isEmpty()
    }
}