package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.ActivityFilterAllTypeDto
import com.rarible.protocol.dto.ActivityFilterByCollectionTypeDto
import com.rarible.protocol.dto.ActivityFilterByItemTypeDto
import com.rarible.protocol.dto.ActivityFilterByUserTypeDto
import com.rarible.protocol.dto.OrderActivityMatchSideDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionActivityTypeDto
import com.rarible.protocol.union.dto.UnionBurnActivityDto
import com.rarible.protocol.union.dto.UnionMintActivityDto
import com.rarible.protocol.union.dto.UnionOrderActivityMatchSideDto
import com.rarible.protocol.union.dto.UnionOrderBidActivityDto
import com.rarible.protocol.union.dto.UnionOrderCancelBidActivityDto
import com.rarible.protocol.union.dto.UnionOrderCancelListActivityDto
import com.rarible.protocol.union.dto.UnionOrderListActivityDto
import com.rarible.protocol.union.dto.UnionOrderMatchActivityDto
import com.rarible.protocol.union.dto.UnionTransferActivityDto
import com.rarible.protocol.union.dto.UnionUserActivityTypeDto
import com.rarible.protocol.union.test.data.randomEthItemBurnActivity
import com.rarible.protocol.union.test.data.randomEthItemMintActivity
import com.rarible.protocol.union.test.data.randomEthItemTransferActivity
import com.rarible.protocol.union.test.data.randomEthOrderActivityCancelBid
import com.rarible.protocol.union.test.data.randomEthOrderActivityCancelList
import com.rarible.protocol.union.test.data.randomEthOrderActivityMatch
import com.rarible.protocol.union.test.data.randomEthOrderBidActivity
import com.rarible.protocol.union.test.data.randomEthOrderListActivity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthUnionActivityConverterTest {

    @Test
    fun `eth order activity match side`() {
        val dto = randomEthOrderActivityMatch()
        val converted = EthUnionActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as UnionOrderMatchActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertMatchSide(converted.left, dto.left)
        assertMatchSide(converted.right, dto.right)
        assertThat(converted.price).isEqualTo(dto.price)
        assertThat(converted.priceUsd).isEqualTo(dto.priceUsd)
        assertThat(converted.source?.name).isEqualTo(dto.source.name)
        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `eth order activity bid`() {
        val dto = randomEthOrderBidActivity()
        val converted = EthUnionActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as UnionOrderBidActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.price).isEqualTo(dto.price)
        assertThat(converted.priceUsd).isEqualTo(dto.priceUsd)
        assertThat(converted.source?.name).isEqualTo(dto.source.name)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
    }

    @Test
    fun `eth order activity list`() {
        val dto = randomEthOrderListActivity()
        val converted = EthUnionActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as UnionOrderListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.price).isEqualTo(dto.price)
        assertThat(converted.priceUsd).isEqualTo(dto.priceUsd)
        assertThat(converted.source?.name).isEqualTo(dto.source.name)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
    }

    @Test
    fun `eth order activity cancel bid`() {
        val dto = randomEthOrderActivityCancelBid()
        val converted =
            EthUnionActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as UnionOrderCancelBidActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.source?.name).isEqualTo(dto.source.name)
        assertThat(converted.hash).isEqualTo(dto.hash.prefixed())
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `eth order activity cancel list`() {
        val dto = randomEthOrderActivityCancelList()
        val converted =
            EthUnionActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as UnionOrderCancelListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.source?.name).isEqualTo(dto.source.name)
        assertThat(converted.hash).isEqualTo(dto.hash.prefixed())
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.blockchainInfo?.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo?.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo?.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo?.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `eth item activity mint`() {
        val dto = randomEthItemMintActivity()
        val converted = EthUnionActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as UnionMintActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owners[0].value).isEqualTo(dto.owner.prefixed())
        assertThat(converted.contract.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `eth item activity burn`() {
        val dto = randomEthItemBurnActivity()
        val converted = EthUnionActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as UnionBurnActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owners[0].value).isEqualTo(dto.owner.prefixed())
        assertThat(converted.contract.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `eth item activity transfer`() {
        val dto = randomEthItemTransferActivity()
        val converted = EthUnionActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as UnionTransferActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owners[0].value).isEqualTo(dto.owner.prefixed())
        assertThat(converted.contract.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `eth activity type as user activity type`() {
        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterByUserTypeDto.BURN)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.BUY))
            .isEqualTo(ActivityFilterByUserTypeDto.BUY)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.GET_BID))
            .isEqualTo(ActivityFilterByUserTypeDto.GET_BID)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterByUserTypeDto.LIST)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.MAKE_BID))
            .isEqualTo(ActivityFilterByUserTypeDto.MAKE_BID)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterByUserTypeDto.MINT)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterByUserTypeDto.SELL)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.TRANSFER_FROM))
            .isEqualTo(ActivityFilterByUserTypeDto.TRANSFER_FROM)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.TRANSFER_TO))
            .isEqualTo(ActivityFilterByUserTypeDto.TRANSFER_TO)
    }

    @Test
    fun `eth activity type as item activity type`() {
        assertThat(EthUnionActivityConverter.asItemActivityType(UnionActivityTypeDto.BID))
            .isEqualTo(ActivityFilterByItemTypeDto.BID)

        assertThat(EthUnionActivityConverter.asItemActivityType(UnionActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterByItemTypeDto.BURN)

        assertThat(EthUnionActivityConverter.asItemActivityType(UnionActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterByItemTypeDto.LIST)

        assertThat(EthUnionActivityConverter.asItemActivityType(UnionActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterByItemTypeDto.MINT)

        assertThat(EthUnionActivityConverter.asItemActivityType(UnionActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterByItemTypeDto.MATCH)

        assertThat(EthUnionActivityConverter.asItemActivityType(UnionActivityTypeDto.TRANSFER))
            .isEqualTo(ActivityFilterByItemTypeDto.TRANSFER)
    }

    @Test
    fun `eth activity type as collection activity type`() {
        assertThat(EthUnionActivityConverter.asCollectionActivityType(UnionActivityTypeDto.BID))
            .isEqualTo(ActivityFilterByCollectionTypeDto.BID)

        assertThat(EthUnionActivityConverter.asCollectionActivityType(UnionActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterByCollectionTypeDto.BURN)

        assertThat(EthUnionActivityConverter.asCollectionActivityType(UnionActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterByCollectionTypeDto.LIST)

        assertThat(EthUnionActivityConverter.asCollectionActivityType(UnionActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterByCollectionTypeDto.MINT)

        assertThat(EthUnionActivityConverter.asCollectionActivityType(UnionActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterByCollectionTypeDto.MATCH)

        assertThat(EthUnionActivityConverter.asCollectionActivityType(UnionActivityTypeDto.TRANSFER))
            .isEqualTo(ActivityFilterByCollectionTypeDto.TRANSFER)
    }

    @Test
    fun `eth activity type as global activity type`() {
        assertThat(EthUnionActivityConverter.asGlobalActivityType(UnionActivityTypeDto.BID))
            .isEqualTo(ActivityFilterAllTypeDto.BID)

        assertThat(EthUnionActivityConverter.asGlobalActivityType(UnionActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterAllTypeDto.BURN)

        assertThat(EthUnionActivityConverter.asGlobalActivityType(UnionActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterAllTypeDto.LIST)

        assertThat(EthUnionActivityConverter.asGlobalActivityType(UnionActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterAllTypeDto.MINT)

        assertThat(EthUnionActivityConverter.asGlobalActivityType(UnionActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterAllTypeDto.SELL)

        assertThat(EthUnionActivityConverter.asGlobalActivityType(UnionActivityTypeDto.TRANSFER))
            .isEqualTo(ActivityFilterAllTypeDto.TRANSFER)
    }

    private fun assertMatchSide(dest: UnionOrderActivityMatchSideDto, expected: OrderActivityMatchSideDto) {
        assertThat(dest.hash).isEqualTo(expected.hash.prefixed())
        assertThat(dest.maker.value).isEqualTo(expected.maker.prefixed())
    }
}