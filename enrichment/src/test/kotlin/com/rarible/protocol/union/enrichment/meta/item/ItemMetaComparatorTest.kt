package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ItemMetaComparatorTest {

    private val itemId = randomEthItemId()

    private val comparator = StrictItemMetaComparator(DefaultItemMetaComparator())

    @Test
    fun `has changed - true, data changed`() {
        val previous = randomUnionMeta()
        val actual = previous.copy(name = "name")

        assertThat(comparator.hasChanged(itemId, previous, actual)).isTrue()
    }

    @Test
    fun `has changed - false, sources are different`() {
        val meta1 = randomUnionMeta()
        val meta2 = randomUnionMeta(source = null)
        val meta3 = randomUnionMeta(source = MetaSource.SIMPLE_HASH)

        assertThat(comparator.hasChanged(itemId, meta1, meta2)).isFalse()
        assertThat(comparator.hasChanged(itemId, meta2, meta1)).isFalse()

        assertThat(comparator.hasChanged(itemId, meta3, meta1)).isFalse()
        assertThat(comparator.hasChanged(itemId, meta1, meta3)).isFalse()

        assertThat(comparator.hasChanged(itemId, meta3, meta2)).isFalse()
        assertThat(comparator.hasChanged(itemId, meta2, meta3)).isFalse()
    }

    @Test
    fun `has changed - false, contributors are different`() {
        val meta1 = randomUnionMeta()
        val meta2 = randomUnionMeta(contributors = listOf(MetaSource.SIMPLE_HASH))

        assertThat(comparator.hasChanged(itemId, meta1, meta2)).isFalse()
        assertThat(comparator.hasChanged(itemId, meta2, meta1)).isFalse()
    }

    @Test
    fun `has changed - false, meta is the same`() {
        val previous = randomUnionMeta()
        val actual = previous.copy(createdAt = previous.createdAt!!.plusSeconds(1))

        assertThat(comparator.hasChanged(itemId, previous, actual)).isFalse()
    }
}
