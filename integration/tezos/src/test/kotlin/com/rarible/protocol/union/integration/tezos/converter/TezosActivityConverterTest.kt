package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.dipdup.client.core.model.DipDupMintActivity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemMintActivity
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import com.rarible.protocol.union.test.mock.CurrencyMock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TezosActivityConverterTest {

    private val dipdupActivityConverter = DipDupActivityConverter(CurrencyMock.currencyServiceMock)

    @Test
    fun `tezos item activity mint`() = runBlocking<Unit> {
        val dto = randomTezosItemMintActivity()
        val actType = dto as DipDupMintActivity
        val converted = dipdupActivityConverter.convert(dto, BlockchainDto.TEZOS) as MintActivityDto

        assertThat(converted.id.value).isEqualTo(actType.id)
        assertThat(converted.date).isEqualTo(actType.date.toInstant())

        assertThat(converted.owner.value).isEqualTo(actType.owner)
        assertThat(converted.contract!!.value).isEqualTo(actType.contract)
        assertThat(converted.tokenId).isEqualTo(actType.tokenId)
        assertThat(converted.itemId).isEqualTo(ItemIdDto(BlockchainDto.TEZOS, actType.contract, actType.tokenId))
        assertThat(converted.value).isEqualTo(actType.value.toBigInteger())
        assertThat(converted.transactionHash).isEqualTo(actType.transactionId)
    }

}
