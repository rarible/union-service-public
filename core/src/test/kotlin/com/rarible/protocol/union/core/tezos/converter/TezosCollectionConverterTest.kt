package com.rarible.protocol.union.core.tezos.converter

import com.rarible.protocol.tezos.dto.NftCollectionFeatureDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.test.data.randomTezosCollectionDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TezosCollectionConverterTest {

    @Test
    fun `tezos collection`() {
        val dto = randomTezosCollectionDto()
            .copy(features = NftCollectionFeatureDto.values().asList())

        val converted = TezosCollectionConverter.convert(dto, BlockchainDto.TEZOS)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.symbol).isEqualTo(dto.symbol)
        assertThat(converted.type).isEqualTo(CollectionDto.Type.TEZOS)
        assertThat(converted.owner!!.value).isEqualTo(dto.owner!!)
        assertThat(converted.features.map { it.name }).isEqualTo(dto.features.map { it.name })
    }
}