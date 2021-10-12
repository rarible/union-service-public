package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.union.core.service.contract
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.test.data.randomFlowBurnDto
import com.rarible.protocol.union.test.data.randomFlowCancelListActivityDto
import com.rarible.protocol.union.test.data.randomFlowMintDto
import com.rarible.protocol.union.test.data.randomFlowNftOrderActivityListDto
import com.rarible.protocol.union.test.data.randomFlowNftOrderActivitySell
import com.rarible.protocol.union.test.data.randomFlowTransferDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlowActivityConverterTest {

    @Test
    fun `flow order activity match`() {
        val dto = randomFlowNftOrderActivitySell()
        val converted = FlowActivityConverter.convert(dto, BlockchainDto.FLOW) as OrderMatchActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted is OrderMatchSellDto)
        //todo assert match is ok after flow implementation
        assertThat(converted.date).isEqualTo(dto.date)
        //assertThat(converted.priceUsd).isEqualTo(dto.priceUsd) //TODO - add usdPrice
        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `flow order activity list`() {
        val dto = randomFlowNftOrderActivityListDto()
        val converted = FlowActivityConverter.convert(dto, BlockchainDto.FLOW) as OrderListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.price).isEqualTo(dto.price)
        //assertThat(converted.priceUsd).isEqualTo(dto.priceUsd) //TODO - add usdPrice
        assertThat(converted.hash).isEqualTo(dto.hash)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        // assertThat(converted.make.value).isEqualTo(dto.make.value) // TODO - types incompatible
        val makeType = converted.make.type as FlowAssetTypeFtDto
        assertThat(makeType.contract.value).isEqualTo(dto.make.contract)
        // assertThat(converted.take.value).isEqualTo(dto.take.value) // TODO - types incompatible
        val takeType = converted.take.type as FlowAssetTypeFtDto
        assertThat(takeType.contract.value).isEqualTo(dto.take.contract)
    }

    @Test
    fun `flow order activity cancel list`() {
        val dto = randomFlowCancelListActivityDto()
        val converted =
            FlowActivityConverter.convert(dto, BlockchainDto.FLOW) as OrderCancelListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.hash).isEqualTo(dto.hash)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        // assertThat(converted.make.value).isEqualTo(dto.make.value) // TODO - types incompatible
        val makeType = converted.make as FlowAssetTypeFtDto
        assertThat(makeType.contract.value).isEqualTo(dto.make.contract)
        // assertThat(converted.take.value).isEqualTo(dto.take.value) // TODO - types incompatible
        val takeType = converted.take as FlowAssetTypeFtDto
        assertThat(takeType.contract.value).isEqualTo(dto.take.contract)
    }

    @Test
    fun `flow item activity mint`() {
        val dto = randomFlowMintDto()
        val converted = FlowActivityConverter.convert(dto, BlockchainDto.FLOW) as MintActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owner.value).isEqualTo(dto.owner)
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
        val converted = FlowActivityConverter.convert(dto, BlockchainDto.FLOW) as TransferActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owner.value).isEqualTo(dto.owner)
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
        val converted = FlowActivityConverter.convert(dto, BlockchainDto.FLOW) as BurnActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owner.value).isEqualTo(dto.owner)
        assertThat(converted.contract.value).isEqualTo(dto.contract)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)

        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }
}