package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOwnershipDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TezosOwnershipConverterTest {

    @Test
    fun `tezos ownership`() {
        val dto = randomTezosOwnershipDto()

        val converted = TezosOwnershipConverter.convert(dto, BlockchainDto.TEZOS)

        assertThat(converted.id.contract).isEqualTo(dto.contract)
        assertThat(converted.id.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.id.owner.value).isEqualTo(dto.owner)

        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.createdAt).isEqualTo(dto.date)
        assertThat(converted.lazyValue).isEqualTo(dto.lazyValue)
        assertThat(converted.creators[0].account.value).isEqualTo(dto.creators[0].account)
        assertThat(converted.creators[0].value).isEqualTo(dto.creators[0].value)
    }

}