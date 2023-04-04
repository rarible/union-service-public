package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.integration.tezos.data.randomDipDupCollection
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupCollectionConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DipDupCollectionConverterTest {

    @Test
    fun `tezos collection`() {
        val id = randomString()
        val dto = randomDipDupCollection(id)
        val converted = DipDupCollectionConverter.convert(dto)

        assertThat(converted.id.value).isEqualTo(id)
        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.symbol).isEqualTo(dto.symbol)
        assertThat(converted.type).isEqualTo(UnionCollection.Type.TEZOS_MT)
        assertThat(converted.features).contains(UnionCollection.Features.BURN)
        assertThat(converted.features).contains(UnionCollection.Features.SECONDARY_SALE_FEES)
        assertThat(converted.meta?.name).isEqualTo(dto.name)
        assertThat(converted.meta?.description).isEqualTo(dto.meta!!.description)
        assertThat(converted.meta?.externalUri).isEqualTo(dto.meta!!.homepage)
        assertThat(converted.meta?.content).contains(
            UnionMetaContent(
                dto.meta!!.content[0].uri,
                MetaContentDto.Representation.ORIGINAL
            )
        )
    }
}
