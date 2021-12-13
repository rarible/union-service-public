package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthOwnershipConverterTest {

    @Test
    fun `eth ownership`() {
        val dto = randomEthOwnershipDto()

        val converted = EthOwnershipConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.id.contract).isEqualTo(dto.contract.prefixed())
        assertThat(converted.id.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.id.owner.value).isEqualTo(dto.owner.prefixed())

        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.createdAt).isEqualTo(dto.date)
        assertThat(converted.lazyValue).isEqualTo(dto.lazyValue)
        assertThat(converted.creators[0].account.value).isEqualTo(dto.creators!![0].account.prefixed())
        assertThat(converted.creators[0].value).isEqualTo(dto.creators!![0].value)
        assertThat(converted.pending.size).isEqualTo(dto.pending.size)
    }

}