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
                UnionMetaAttribute("key3", "c"),
                UnionMetaAttribute("key4", "d"),
            )
        )

        val helper = ItemMetaCustomizerHelper(randomItemId(), meta)

        val filtered = helper.filterAttributes(
            setOf("key1", "key2", "key3", "key5"),
            mapOf(
                "key2" to setOf("b"),
                "key3" to setOf("z")
            )
        )

        assertThat(filtered).isEqualTo(
            listOf(
                UnionMetaAttribute("key1", "a"),
                UnionMetaAttribute("key3", "c"),
            )
        )
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
