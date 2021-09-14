package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.test.data.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthUnionActivityConverterTest {

    @Test
    fun `order activity match side`() {
        val dto = randomEthOrderActivityMatch()
        val converted = EthUnionActivityConverter.convert(dto, EthBlockchainDto.ETHEREUM) as EthOrderMatchActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertMatchSide(converted.left, dto.left)
        assertMatchSide(converted.right, dto.right)
        assertThat(converted.price).isEqualTo(dto.price)
        assertThat(converted.priceUsd).isEqualTo(dto.priceUsd)
        assertThat(converted.source.name).isEqualTo(dto.source.name)
        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `order activity bid`() {
        val dto = randomEthOrderBidActivity()
        val converted = EthUnionActivityConverter.convert(dto, EthBlockchainDto.ETHEREUM) as EthOrderBidActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.price).isEqualTo(dto.price)
        assertThat(converted.priceUsd).isEqualTo(dto.priceUsd)
        assertThat(converted.source.name).isEqualTo(dto.source.name)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
    }

    @Test
    fun `order activity list`() {
        val dto = randomEthOrderListActivity()
        val converted = EthUnionActivityConverter.convert(dto, EthBlockchainDto.ETHEREUM) as EthOrderListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.price).isEqualTo(dto.price)
        assertThat(converted.priceUsd).isEqualTo(dto.priceUsd)
        assertThat(converted.source.name).isEqualTo(dto.source.name)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
    }

    @Test
    fun `order activity cancel bid`() {
        val dto = randomEthOrderActivityCancelBid()
        val converted =
            EthUnionActivityConverter.convert(dto, EthBlockchainDto.ETHEREUM) as EthOrderCancelBidActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.source.name).isEqualTo(dto.source.name)
        assertThat(converted.hash).isEqualTo(dto.hash.prefixed())
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `order activity cancel list`() {
        val dto = randomEthOrderActivityCancelList()
        val converted =
            EthUnionActivityConverter.convert(dto, EthBlockchainDto.ETHEREUM) as EthOrderCancelListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.source.name).isEqualTo(dto.source.name)
        assertThat(converted.hash).isEqualTo(dto.hash.prefixed())
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.blockchainInfo.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `item activity mint`() {
        val dto = randomEthItemMintActivity()
        val converted = EthUnionActivityConverter.convert(dto, EthBlockchainDto.ETHEREUM) as EthMintActivityDto

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
    fun `item activity burn`() {
        val dto = randomEthItemBurnActivity()
        val converted = EthUnionActivityConverter.convert(dto, EthBlockchainDto.ETHEREUM) as EthBurnActivityDto

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
    fun `item activity transfer`() {
        val dto = randomEthItemTransferActivity()
        val converted = EthUnionActivityConverter.convert(dto, EthBlockchainDto.ETHEREUM) as EthTransferActivityDto

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
    fun `as user activity type`() {
        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterByUserDto.Types.BURN)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.BUY))
            .isEqualTo(ActivityFilterByUserDto.Types.BUY)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.GET_BID))
            .isEqualTo(ActivityFilterByUserDto.Types.GET_BID)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterByUserDto.Types.LIST)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.MAKE_BID))
            .isEqualTo(ActivityFilterByUserDto.Types.MAKE_BID)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterByUserDto.Types.MINT)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterByUserDto.Types.SELL)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.TRANSFER_FROM))
            .isEqualTo(ActivityFilterByUserDto.Types.TRANSFER_FROM)

        assertThat(EthUnionActivityConverter.asUserActivityType(UnionUserActivityTypeDto.TRANSFER_TO))
            .isEqualTo(ActivityFilterByUserDto.Types.TRANSFER_TO)
    }

    @Test
    fun `as item activity type`() {
        assertThat(EthUnionActivityConverter.asItemActivityType(UnionActivityTypeDto.BID))
            .isEqualTo(ActivityFilterByItemDto.Types.BID)

        assertThat(EthUnionActivityConverter.asItemActivityType(UnionActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterByItemDto.Types.BURN)

        assertThat(EthUnionActivityConverter.asItemActivityType(UnionActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterByItemDto.Types.LIST)

        assertThat(EthUnionActivityConverter.asItemActivityType(UnionActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterByItemDto.Types.MINT)

        assertThat(EthUnionActivityConverter.asItemActivityType(UnionActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterByItemDto.Types.MATCH)

        assertThat(EthUnionActivityConverter.asItemActivityType(UnionActivityTypeDto.TRANSFER))
            .isEqualTo(ActivityFilterByItemDto.Types.TRANSFER)
    }

    @Test
    fun `as collection activity type`() {
        assertThat(EthUnionActivityConverter.asCollectionActivityType(UnionActivityTypeDto.BID))
            .isEqualTo(ActivityFilterByCollectionDto.Types.BID)

        assertThat(EthUnionActivityConverter.asCollectionActivityType(UnionActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterByCollectionDto.Types.BURN)

        assertThat(EthUnionActivityConverter.asCollectionActivityType(UnionActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterByCollectionDto.Types.LIST)

        assertThat(EthUnionActivityConverter.asCollectionActivityType(UnionActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterByCollectionDto.Types.MINT)

        assertThat(EthUnionActivityConverter.asCollectionActivityType(UnionActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterByCollectionDto.Types.MATCH)

        assertThat(EthUnionActivityConverter.asCollectionActivityType(UnionActivityTypeDto.TRANSFER))
            .isEqualTo(ActivityFilterByCollectionDto.Types.TRANSFER)
    }

    @Test
    fun `as global activity type`() {
        assertThat(EthUnionActivityConverter.asGlobalActivityType(UnionActivityTypeDto.BID))
            .isEqualTo(ActivityFilterAllDto.Types.BID)

        assertThat(EthUnionActivityConverter.asGlobalActivityType(UnionActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterAllDto.Types.BURN)

        assertThat(EthUnionActivityConverter.asGlobalActivityType(UnionActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterAllDto.Types.LIST)

        assertThat(EthUnionActivityConverter.asGlobalActivityType(UnionActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterAllDto.Types.MINT)

        assertThat(EthUnionActivityConverter.asGlobalActivityType(UnionActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterAllDto.Types.SELL)

        assertThat(EthUnionActivityConverter.asGlobalActivityType(UnionActivityTypeDto.TRANSFER))
            .isEqualTo(ActivityFilterAllDto.Types.TRANSFER)
    }

    private fun assertMatchSide(dest: EthOrderActivityMatchSideDto, expected: OrderActivityMatchSideDto) {
        assertThat(dest.hash).isEqualTo(expected.hash.prefixed())
        assertThat(dest.maker.value).isEqualTo(expected.maker.prefixed())
        assertThat(dest.type.name).isEqualTo(expected.type!!.name)
    }

}