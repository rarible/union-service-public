package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowEventTimeMarksDto
import com.rarible.protocol.union.core.model.UnionFlowAssetTypeFt
import com.rarible.protocol.union.core.model.UnionFlowAssetTypeNft
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.data.randomFlowEventTimeMarks
import com.rarible.protocol.union.integration.flow.data.randomFlowFungibleAsset
import com.rarible.protocol.union.integration.flow.data.randomFlowNftAsset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowConverterTest {

    @Test
    fun `asset - FT`() {
        val dto = randomFlowFungibleAsset()

        val converted = FlowConverter.convert(dto, BlockchainDto.FLOW)

        // assertThat(converted.value.toBigDecimal()).isEqualTo(dto.value) // TODO - types incompatible
        val type = converted.type as UnionFlowAssetTypeFt
        assertThat(type.contract.value).isEqualTo(dto.contract)
    }

    @Test
    fun `asset - NFT`() {
        val dto = randomFlowNftAsset()

        val converted = FlowConverter.convert(dto, BlockchainDto.FLOW)

        // assertThat(converted.value.toBigDecimal()).isEqualTo(dto.value) // TODO - types incompatible
        val type = converted.type as UnionFlowAssetTypeNft
        assertThat(type.contract.value).isEqualTo(dto.contract)
        assertThat(type.tokenId).isEqualTo(dto.tokenId)
    }

    @Test
    fun `time marks - ok`() {
        val marks = randomFlowEventTimeMarks()
        val converted = FlowConverter.convert(marks)!!

        assertThat(converted.source).isEqualTo(marks.source)
        assertThat(converted.marks).hasSize(marks.marks.size)
        assertThat(converted.marks[0].name).isEqualTo(marks.marks[0].name)
        assertThat(converted.marks[0].date).isEqualTo(marks.marks[0].date)
    }

    @Test
    fun `time marks - null`() {
        val marks: FlowEventTimeMarksDto? = null
        assertThat(FlowConverter.convert(marks)).isNull()
    }
}
