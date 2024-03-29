package com.rarible.protocol.union.enrichment.meta.item.customizer

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.enrichment.repository.ItemMetaCustomAttributesRepository
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.enrichment.test.data.randomUnionMetaAttribute
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ItemMetaAttributeCustomizerTest {

    @MockK
    lateinit var itemMetaCustomAttributeRepository: ItemMetaCustomAttributesRepository

    @InjectMockKs
    lateinit var customizer: ItemMetaAttributeCustomizer

    @Test
    fun `customize - ok, attributes added`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val meta = randomUnionMeta()
        val extraAttribute = randomUnionMetaAttribute()

        coEvery { itemMetaCustomAttributeRepository.getCustomAttributes(itemId) } returns listOf(extraAttribute)

        val result = customizer.customize(itemId, meta)

        assertThat(result.attributes).isEqualTo(meta.attributes + listOf(extraAttribute))
    }

    @Test
    fun `customize - ok, attributes replaced`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val meta = randomUnionMeta()
        val extraAttribute = meta.attributes[0].copy(value = randomString())

        coEvery { itemMetaCustomAttributeRepository.getCustomAttributes(itemId) } returns listOf(extraAttribute)

        val result = customizer.customize(itemId, meta)

        assertThat(result.attributes).isEqualTo(listOf(extraAttribute))
    }
}
