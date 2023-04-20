package com.rarible.protocol.union.core.converter.helper

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.core.model.elastic.EsOrder
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class EsOrderConverterTest {

    private val converter = EsOrderConverter

    @Test
    fun `should convert to SELL type`() {
        val assetType = AssetDto(
            type = EthErc721AssetTypeDto(
                contract = ContractAddress(BlockchainDto.ETHEREUM, randomString()),
                tokenId = BigInteger.ZERO
            ),
            value = BigDecimal.ONE
        )

        val type = converter.orderType(assetType)
        assertThat(type).isEqualTo(EsOrder.Type.SELL)
    }
}
