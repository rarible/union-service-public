package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowAssetTypeDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import com.rarible.protocol.union.test.data.randomFlowFungibleAsset
import com.rarible.protocol.union.test.data.randomFlowNftAsset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowConverterTest {

    @Test
    fun `asset - FT`() {
        val dto = randomFlowFungibleAsset()

        val converted = FlowConverter.convert(dto, BlockchainDto.FLOW)

        // assertThat(converted.value.toBigDecimal()).isEqualTo(dto.value) // TODO - types incompatible
        val type = converted.type as FlowAssetTypeDto
        assertThat(type.contract.value).isEqualTo(dto.contract)
    }

    @Test
    fun `asset - NFT`() {
        val dto = randomFlowNftAsset()

        val converted = FlowConverter.convert(dto, BlockchainDto.FLOW)

        // assertThat(converted.value.toBigDecimal()).isEqualTo(dto.value) // TODO - types incompatible
        val type = converted.type as FlowAssetTypeDto
        assertThat(type.contract.value).isEqualTo(dto.contract)
        assertThat(type).isInstanceOf(FlowAssetTypeNftDto::class.java)
        assertThat((type as FlowAssetTypeNftDto).tokenId).isEqualTo(dto.tokenId)
    }
}