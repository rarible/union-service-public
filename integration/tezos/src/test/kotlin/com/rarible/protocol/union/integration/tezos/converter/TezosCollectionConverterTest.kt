package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosCollectionDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktCollectionConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TezosCollectionConverterTest {

    @Test
    fun `tezos collection`() {
        val dto = randomTezosCollectionDto()
        val converted = TzktCollectionConverter.convert(dto, BlockchainDto.TEZOS)

        assertThat(converted.id.value).isEqualTo(dto.address)
        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.symbol).isEqualTo(dto.symbol)
        assertThat(converted.type).isEqualTo(CollectionDto.Type.TEZOS_MT)
        assertThat(converted.features).contains(CollectionDto.Features.BURN)
        assertThat(converted.features).contains(CollectionDto.Features.SECONDARY_SALE_FEES)
    }
}
