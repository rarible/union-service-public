package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosDipDupOwnershipDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosTzktOwnershipDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOwnershipConverter
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktOwnershipConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class TezosOwnershipConverterTest {

    @Test
    fun `tezos tzkt ownership`() {
        val dto = randomTezosTzktOwnershipDto()

        val converted = TzktOwnershipConverter.convert(dto, BlockchainDto.TEZOS)

        assertThat(converted.id.owner.value).isEqualTo(dto.account!!.address)

        assertThat(converted.collection!!.value).isEqualTo(dto.token!!.contract!!.address)
        assertThat(converted.value).isEqualTo(BigInteger(dto.balance))
        assertThat(converted.createdAt).isEqualTo(dto.firstTime.toInstant())
        assertThat(converted.lastUpdatedAt).isNotNull
        assertThat(converted.creators).isEqualTo(emptyList<CreatorDto>())
    }

    @Test
    fun `tezos dipdup ownership`() {
        val dto = randomTezosDipDupOwnershipDto()

        val converted = DipDupOwnershipConverter.convert(dto)

        assertThat(converted.id.owner.value).isEqualTo(dto.owner)
        assertThat(converted.id.getItemId()).isEqualTo(ItemIdDto(BlockchainDto.TEZOS, dto.contract, dto.tokenId))

        assertThat(converted.value).isEqualTo(dto.balance)
        assertThat(converted.createdAt).isEqualTo(dto.created)
        assertThat(converted.lastUpdatedAt).isNotNull
        assertThat(converted.creators).isEqualTo(emptyList<CreatorDto>())
    }
}
