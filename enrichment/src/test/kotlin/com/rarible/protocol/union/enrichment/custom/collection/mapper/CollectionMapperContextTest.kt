package com.rarible.protocol.union.enrichment.custom.collection.mapper

import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionItemProvider
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class CollectionMapperContextTest {

    @MockK
    private lateinit var provider: CustomCollectionItemProvider

    @BeforeEach
    fun beforeEach() {
        clearMocks(provider)
        coEvery { provider.getOrFetchMeta(emptyList()) } returns emptyMap()
    }

    @Test
    fun `get items meta - ok, cached`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomShortItem(itemId).copy(metaEntry = randomItemMetaDownloadEntry())
        val context = CollectionMapperContext(
            itemHint = mapOf(itemId to item),
            provider = provider
        )

        val result = context.getItemsMeta(listOf(itemId))
        assertThat(result[itemId]).isEqualTo(item.metaEntry!!.data)
    }

    @Test
    fun `get items meta - ok, fetched`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val meta = randomUnionMeta()
        val context = CollectionMapperContext(
            itemHint = emptyMap(),
            provider = provider
        )

        coEvery { provider.getOrFetchMeta(listOf(itemId)) } returns mapOf(itemId to meta)

        val result = context.getItemsMeta(listOf(itemId))
        assertThat(result[itemId]).isEqualTo(meta)

        val result2 = context.getItemsMeta(listOf(itemId))
        assertThat(result2[itemId]).isEqualTo(meta)

        coVerify(exactly = 1) { provider.getOrFetchMeta(listOf(itemId)) }
    }
}
