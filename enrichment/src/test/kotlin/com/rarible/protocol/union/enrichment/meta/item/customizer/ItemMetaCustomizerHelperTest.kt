package com.rarible.protocol.union.enrichment.meta.item.customizer

import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import randomItemId

class ItemMetaCustomizerHelperTest {

    @Test
    fun `filter attributes`() {
        val meta = randomUnionMeta().copy(
            attributes = listOf(
                UnionMetaAttribute("key1", "a"),
                UnionMetaAttribute("key2", "b"),
            )
        )

        val helper = ItemMetaCustomizerHelper(randomItemId(), meta)

        val filtered = helper.filterAttributes(setOf("key1", "key3"))

        assertThat(filtered).isEqualTo(listOf(UnionMetaAttribute("key1", "a")))
    }

    @Test
    fun `get attribute`() {
        val meta = randomUnionMeta().copy(
            attributes = listOf(
                UnionMetaAttribute("key1", "a"),
                UnionMetaAttribute("key2", "b")
            )
        )
        val helper = ItemMetaCustomizerHelper(randomItemId(), meta)

        assertThat(helper.attribute("key", "key1", "key2")).isEqualTo("a")
        assertThat(helper.attribute("key")).isNull()
    }

}