package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.test.data.randomEthNftOwnershipDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthUnionOwnershipConverterTest {

    @Test
    fun ownership() {
        val dto = randomEthNftOwnershipDto()

        val converted = EthUnionOwnershipConverter.convert(dto, EthBlockchainDto.ETHEREUM)

        assertThat(converted.id.token.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.id.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.id.owner.value).isEqualTo(dto.owner.prefixed())

        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.createdAt).isEqualTo(dto.date)
        assertThat(converted.lazyValue).isEqualTo(dto.lazyValue)
        assertThat(converted.owners[0].value).isEqualTo(dto.owner.prefixed())
        assertThat(converted.creators[0].account.value).isEqualTo(dto.creators[0].account.prefixed())
        assertThat(converted.creators[0].value).isEqualTo(dto.creators[0].value.toBigDecimal())
        assertThat(converted.pending.size).isEqualTo(dto.pending.size)
    }

}