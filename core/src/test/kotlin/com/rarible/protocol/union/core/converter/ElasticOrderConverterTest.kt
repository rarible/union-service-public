package com.rarible.protocol.union.core.converter

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.core.model.EsOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class ElasticOrderConverterTest {

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
