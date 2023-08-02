package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.download.MetaProviderType
import com.rarible.protocol.union.core.model.download.MetaSource
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionContent
import com.rarible.protocol.union.enrichment.test.data.randomUnionImageProperties
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ItemMetaComparatorTest {

    private val itemId = randomEthItemId()

    @Test
    fun `has changed - true, name changed`() {
        val previous = randomUnionMeta()
        val actual = previous.copy(name = "name")

        assertThat(ItemMetaComparator.hasChanged(itemId, previous, actual)).isTrue()
    }

    @Test
    fun `has changed - true, description changed`() {
        val previous = randomUnionMeta()
        val actual = previous.copy(description = "description")

        assertThat(ItemMetaComparator.hasChanged(itemId, previous, actual)).isTrue()
    }

    @Test
    fun `has changed - true, meta uri changed`() {
        val previous = randomUnionMeta()
        val actual = previous.copy(originalMetaUri = "originalMetaUri")

        assertThat(ItemMetaComparator.hasChanged(itemId, previous, actual)).isTrue()
    }

    @Test
    fun `has changed - true, content changed`() {
        val content = randomUnionContent(randomUnionImageProperties()).copy(
            representation = MetaContentDto.Representation.ORIGINAL
        )

        val previous = randomUnionMeta(content = listOf(content))
        val actual = previous.copy(content = listOf(content.copy(url = "url")))

        assertThat(ItemMetaComparator.hasChanged(itemId, previous, actual)).isTrue()
    }

    @Test
    fun `has changed - false, sources are different`() {
        val meta1 = randomUnionMeta()
        val meta2 = randomUnionMeta(source = null)
        val meta3 = randomUnionMeta(source = MetaSource.SIMPLE_HASH)

        assertThat(ItemMetaComparator.hasChanged(itemId, meta1, meta2)).isFalse()
        assertThat(ItemMetaComparator.hasChanged(itemId, meta2, meta1)).isFalse()

        assertThat(ItemMetaComparator.hasChanged(itemId, meta3, meta1)).isFalse()
        assertThat(ItemMetaComparator.hasChanged(itemId, meta1, meta3)).isFalse()

        assertThat(ItemMetaComparator.hasChanged(itemId, meta3, meta2)).isFalse()
        assertThat(ItemMetaComparator.hasChanged(itemId, meta2, meta3)).isFalse()
    }

    @Test
    fun `has changed - false, contributors are different`() {
        val meta1 = randomUnionMeta()
        val meta2 = randomUnionMeta(contributors = listOf(MetaProviderType.SIMPLE_HASH))

        assertThat(ItemMetaComparator.hasChanged(itemId, meta1, meta2)).isFalse()
        assertThat(ItemMetaComparator.hasChanged(itemId, meta2, meta1)).isFalse()
    }

    @Test
    fun `has changed - false, original content hasn't been changed`() {
        val content = randomUnionContent(randomUnionImageProperties()).copy(
            representation = MetaContentDto.Representation.BIG
        )

        val previous = randomUnionMeta(content = listOf(content))
        val actual = previous.copy(content = listOf(content.copy(url = "url")))

        assertThat(ItemMetaComparator.hasChanged(itemId, previous, actual)).isFalse()
    }
}
