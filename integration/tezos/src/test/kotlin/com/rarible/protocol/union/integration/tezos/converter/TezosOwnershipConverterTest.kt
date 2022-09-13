package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOwnershipDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktOwnershipConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class TezosOwnershipConverterTest {

    @Test
    fun `tezos ownership`() {
        val dto = randomTezosOwnershipDto()

        val converted = TzktOwnershipConverter.convert(dto, BlockchainDto.TEZOS)

        assertThat(converted.id.owner.value).isEqualTo(dto.account!!.address)

        assertThat(converted.collection!!.value).isEqualTo(dto.token!!.contract!!.address)
        assertThat(converted.value).isEqualTo(BigInteger(dto.balance))
        assertThat(converted.createdAt).isEqualTo(dto.firstTime.toInstant())
        assertThat(converted.lastUpdatedAt).isNotNull
        assertThat(converted.creators).isEqualTo(emptyList<CreatorDto>())
    }

}
