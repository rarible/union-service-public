package com.rarible.protocol.union.enrichment.meta.collection.provider

import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.enrichment.download.ProviderDownloadException
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaMetrics
import com.rarible.protocol.union.enrichment.service.MarketplaceService
import com.rarible.protocol.union.enrichment.test.data.randomMarketplaceTokenDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionCollectionMeta
import com.rarible.protocol.union.enrichment.test.data.randomUnionContent
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class MarketplaceCollectionMetaProviderTest {

    @MockK
    private lateinit var marketplaceService: MarketplaceService

    private val metrics: CollectionMetaMetrics = CollectionMetaMetrics(SimpleMeterRegistry())

    @InjectMockKs
    private lateinit var provider: MarketplaceCollectionMetaProvider

    @BeforeEach
    private fun beforeEach() {
        clearMocks(marketplaceService)
        every { marketplaceService.isSupported(any()) } returns true
    }

    @Test
    fun `fetch meta - ok, original is null`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        val meta = randomMarketplaceTokenDto()

        coEvery { marketplaceService.getCollection(collectionId) } returns meta

        val result = provider.fetch(collectionId.blockchain, collectionId.value, null)!!

        assertThat(result.name).isEqualTo(meta.name)
        assertThat(result.description).isEqualTo(meta.description)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].url).isEqualTo(meta.pic)
    }

    @Test
    fun `fetch meta - ok, original without content`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        val meta = randomMarketplaceTokenDto()

        coEvery { marketplaceService.getCollection(collectionId) } returns meta

        val result = provider.fetch(collectionId.blockchain, collectionId.value, randomUnionCollectionMeta())!!

        assertThat(result.name).isEqualTo(meta.name)
        assertThat(result.description).isEqualTo(meta.description)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].url).isEqualTo(meta.pic)
    }

    @Test
    fun `fetch meta - ok, original with content`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        val original = randomUnionCollectionMeta().copy(content = listOf(randomUnionContent()))

        val result = provider.fetch(collectionId.blockchain, collectionId.value, original)!!

        assertThat(result).isEqualTo(original)
    }

    @Test
    fun `fetch meta - fail`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()

        coEvery { marketplaceService.getCollection(collectionId) } throws RuntimeException()

        val ex = assertThrows<ProviderDownloadException> {
            provider.fetch(collectionId.blockchain, collectionId.value, null)
        }

        assertThat(ex.provider).isEqualTo(MetaSource.MARKETPLACE)
    }
}
