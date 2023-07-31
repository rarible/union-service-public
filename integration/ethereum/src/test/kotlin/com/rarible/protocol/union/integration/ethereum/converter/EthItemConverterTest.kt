package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemRoyaltyDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemTransferDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthItemConverterTest {

    @Test
    fun `eth item history - transfer`() {
        val dto = randomEthItemTransferDto()

        val converted = EthItemConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.owner.value).isEqualTo(dto.owner.prefixed())
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.contract.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)

        assertThat(converted.from.value).isEqualTo(dto.from.prefixed())
    }

    @Test
    fun `eth item history - royalty`() {
        val dto = randomEthItemRoyaltyDto()

        val converted = EthItemConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.owner!!.value).isEqualTo(dto.owner!!.prefixed())
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.contract.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)

        assertThat(converted.royalties[0].account.value).isEqualTo(dto.royalties[0].account.prefixed())
        assertThat(converted.royalties[0].value).isEqualTo(dto.royalties[0].value)
    }

    @Test
    fun `eth item`() {
        val dto = randomEthNftItemDto()

        val converted = EthItemConverter.convert(dto, BlockchainDto.ETHEREUM)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.collection!!.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.supply).isEqualTo(dto.supply)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdatedAt)
        assertThat(converted.mintedAt).isEqualTo(dto.mintedAt)
        assertThat(converted.lazySupply).isEqualTo(dto.lazySupply)
        assertThat(converted.deleted).isEqualTo(dto.deleted)
        assertThat(converted.lazySupply).isEqualTo(dto.lazySupply)

        assertThat(converted.creators[0].account.value).isEqualTo(dto.creators[0].account.prefixed())
        assertThat(converted.creators[0].value).isEqualTo(dto.creators[0].value)

        assertThat(converted.pending[0].from.value).isEqualTo(dto.pending!![0].from.prefixed())
        assertThat(converted.pending[0].owner.value).isEqualTo(dto.pending!![0].owner.prefixed())
        assertThat(converted.pending[0].date).isEqualTo(dto.pending!![0].date)
        assertThat(converted.pending[0].value).isEqualTo(dto.pending!![0].value)
        assertThat(converted.pending[0].contract.value).isEqualTo(dto.pending!![0].contract.prefixed())
        assertThat(converted.pending[0].tokenId).isEqualTo(dto.pending!![0].tokenId)
    }
}
