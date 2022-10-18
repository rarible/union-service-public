package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.enrichment.configuration.ItemMetaTrimmingProperties
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.enrichment.test.data.randomUnionMetaAttribute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ItemMetaTrimmerTest {

    private val properties = ItemMetaTrimmingProperties(
        nameLength = 5,
        descriptionLength = 10,
        attributesSize = 2,
        attributeNameLength = 3,
        attributeValueLength = 6
    )

    private val trimmer = ItemMetaTrimmer(properties)

    @Test
    fun `data trimmed`() {

        val attr1 = randomUnionMetaAttribute().copy(key = "1234", value = "1234567")

        val meta = randomUnionMeta().copy(
            name = "123456",
            description = "123456789012345",
            attributes = listOf(attr1, randomUnionMetaAttribute(), randomUnionMetaAttribute())
        )

        val trimmedMeta = trimmer.trim(meta)!!
        val trimmedAttr = trimmedMeta.attributes[0]

        assertThat(trimmedMeta.name).isEqualTo("12345...")
        assertThat(trimmedMeta.description).isEqualTo("1234567890...")

        assertThat(trimmedMeta.attributes).hasSize(2)
        assertThat(trimmedAttr.key).isEqualTo("123...")
        assertThat(trimmedAttr.value).isEqualTo("123456...")
    }

    @Test
    fun `data kept`() {
        val attr1 = randomUnionMetaAttribute().copy(key = "123", value = "123456")

        val meta = randomUnionMeta().copy(
            name = "12345",
            description = "1234567890",
            attributes = listOf(attr1)
        )

        val trimmedMeta = trimmer.trim(meta)!!

        assertThat(trimmedMeta).isEqualTo(meta)
    }
}