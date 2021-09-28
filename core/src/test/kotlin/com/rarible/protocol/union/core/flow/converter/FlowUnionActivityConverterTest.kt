package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowOrderActivityMatchSideDto
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.test.data.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowUnionActivityConverterTest {

    @Test
    fun `flow order activity match`() {
        val dto = randomFlowNftOrderActivitySell()
        val converted = FlowUnionActivityConverter.convert(dto, BlockchainDto.FLOW) as UnionOrderMatchActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertMatchSide(converted.left, dto.left)
        assertMatchSide(converted.right, dto.right)
        assertThat(converted.price).isEqualTo(dto.price)
        //assertThat(converted.priceUsd).isEqualTo(dto.priceUsd) //TODO - add usdPrice
        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `flow order activity list`() {
        val dto = randomFlowNftOrderActivityListDto()
        val converted = FlowUnionActivityConverter.convert(dto, BlockchainDto.FLOW) as UnionOrderListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.price).isEqualTo(dto.price)
        //assertThat(converted.priceUsd).isEqualTo(dto.priceUsd) //TODO - add usdPrice
        assertThat(converted.hash).isEqualTo(dto.hash)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        // assertThat(converted.make.value).isEqualTo(dto.make.value) // TODO - types incompatible
        val makeType = converted.make.type as FlowAssetTypeDto
        assertThat(makeType.contract.value).isEqualTo(dto.make.contract)
        // assertThat(converted.take.value).isEqualTo(dto.take.value) // TODO - types incompatible
        val takeType = converted.take.type as FlowAssetTypeDto
        assertThat(takeType.contract.value).isEqualTo(dto.take.contract)
    }

    @Test
    fun `flow order activity cancel list`() {
        val dto = randomFlowCancelListActivityDto()
        val converted =
            FlowUnionActivityConverter.convert(dto, BlockchainDto.FLOW) as UnionOrderCancelListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.hash).isEqualTo(dto.hash)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        // assertThat(converted.make.value).isEqualTo(dto.make.value) // TODO - types incompatible
        val makeType = converted.make as FlowAssetTypeDto
        assertThat(makeType.contract.value).isEqualTo(dto.make.contract)
        // assertThat(converted.take.value).isEqualTo(dto.take.value) // TODO - types incompatible
        val takeType = converted.take as FlowAssetTypeDto
        assertThat(takeType.contract.value).isEqualTo(dto.take.contract)
    }

    @Test
    fun `flow item activity mint`() {
        val dto = randomFlowMintDto()
        val converted = FlowUnionActivityConverter.convert(dto, BlockchainDto.FLOW) as UnionMintActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owners[0].value).isEqualTo(dto.owner)
        assertThat(converted.contract.value).isEqualTo(dto.contract)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)

        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `flow item activity transfer`() {
        val dto = randomFlowTransferDto()
        val converted = FlowUnionActivityConverter.convert(dto, BlockchainDto.FLOW) as UnionTransferActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owners!![0].value).isEqualTo(dto.owner)
        assertThat(converted.contract.value).isEqualTo(dto.contract)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)

        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `flow item activity burn`() {
        val dto = randomFlowBurnDto()
        val converted = FlowUnionActivityConverter.convert(dto, BlockchainDto.FLOW) as UnionBurnActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owners[0].value).isEqualTo(dto.owner)
        assertThat(converted.contract.value).isEqualTo(dto.contract)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)

        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }

    private fun assertMatchSide(
        dest: UnionOrderActivityMatchSideDto,
        expected: FlowOrderActivityMatchSideDto
    ) {
        //assertThat(dest.asset.value).isEqualTo(expected.asset.value) // TODO - types incompatible
        assertThat(dest.maker.value).isEqualTo(expected.maker)
    }
}