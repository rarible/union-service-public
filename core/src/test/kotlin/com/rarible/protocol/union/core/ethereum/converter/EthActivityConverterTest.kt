package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.ActivityFilterAllTypeDto
import com.rarible.protocol.dto.ActivityFilterByCollectionTypeDto
import com.rarible.protocol.dto.ActivityFilterByItemTypeDto
import com.rarible.protocol.dto.ActivityFilterByUserTypeDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivityMatchSideDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchActivityDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
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

class EthActivityConverterTest {

    @Test
    fun `eth order activity match side`() {
        val dto = randomEthOrderActivityMatch()
        val converted = EthActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as OrderMatchActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.type).isNull()
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
    fun `eth order activity match side - type`() {
        assertThat(EthActivityConverter.convert(null)).isNull()

        assertThat(EthActivityConverter.convert(OrderActivityMatchDto.Type.SELL))
            .isEqualTo(OrderMatchActivityDto.Type.SELL)

        assertThat(EthActivityConverter.convert(OrderActivityMatchDto.Type.ACCEPT_BID))
            .isEqualTo(OrderMatchActivityDto.Type.ACCEPT_BID)
    }

    @Test
    fun `eth order activity bid`() {
        val dto = randomEthOrderBidActivity()
        val converted = EthActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as OrderBidActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.price).isEqualTo(dto.price)
        assertThat(converted.priceUsd).isEqualTo(dto.priceUsd)
        assertThat(converted.source?.name).isEqualTo(dto.source.name)
        assertThat(converted.take.value).isEqualTo(dto.take.valueDecimal)
        assertThat(converted.make.value).isEqualTo(dto.make.valueDecimal)
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
    }

    @Test
    fun `eth order activity list`() {
        val dto = randomEthOrderListActivity()
        val converted = EthActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as OrderListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.price).isEqualTo(dto.price)
        assertThat(converted.priceUsd).isEqualTo(dto.priceUsd)
        assertThat(converted.source?.name).isEqualTo(dto.source.name)
        assertThat(converted.take.value).isEqualTo(dto.take.valueDecimal)
        assertThat(converted.make.value).isEqualTo(dto.make.valueDecimal)
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
    }

    @Test
    fun `eth order activity cancel bid`() {
        val dto = randomEthOrderActivityCancelBid()
        val converted =
            EthActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as OrderCancelBidActivityDto

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
            EthActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as OrderCancelListActivityDto

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
        val converted = EthActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as MintActivityDto

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
        val converted = EthActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as BurnActivityDto

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
        val converted = EthActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as TransferActivityDto

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
        assertThat(EthActivityConverter.asUserActivityType(UserActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterByUserTypeDto.BURN)

        assertThat(EthActivityConverter.asUserActivityType(UserActivityTypeDto.BUY))
            .isEqualTo(ActivityFilterByUserTypeDto.BUY)

        assertThat(EthActivityConverter.asUserActivityType(UserActivityTypeDto.GET_BID))
            .isEqualTo(ActivityFilterByUserTypeDto.GET_BID)

        assertThat(EthActivityConverter.asUserActivityType(UserActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterByUserTypeDto.LIST)

        assertThat(EthActivityConverter.asUserActivityType(UserActivityTypeDto.MAKE_BID))
            .isEqualTo(ActivityFilterByUserTypeDto.MAKE_BID)

        assertThat(EthActivityConverter.asUserActivityType(UserActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterByUserTypeDto.MINT)

        assertThat(EthActivityConverter.asUserActivityType(UserActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterByUserTypeDto.SELL)

        assertThat(EthActivityConverter.asUserActivityType(UserActivityTypeDto.TRANSFER_FROM))
            .isEqualTo(ActivityFilterByUserTypeDto.TRANSFER_FROM)

        assertThat(EthActivityConverter.asUserActivityType(UserActivityTypeDto.TRANSFER_TO))
            .isEqualTo(ActivityFilterByUserTypeDto.TRANSFER_TO)
    }

    @Test
    fun `eth activity type as item activity type`() {
        assertThat(EthActivityConverter.asItemActivityType(ActivityTypeDto.BID))
            .isEqualTo(ActivityFilterByItemTypeDto.BID)

        assertThat(EthActivityConverter.asItemActivityType(ActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterByItemTypeDto.BURN)

        assertThat(EthActivityConverter.asItemActivityType(ActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterByItemTypeDto.LIST)

        assertThat(EthActivityConverter.asItemActivityType(ActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterByItemTypeDto.MINT)

        assertThat(EthActivityConverter.asItemActivityType(ActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterByItemTypeDto.MATCH)

        assertThat(EthActivityConverter.asItemActivityType(ActivityTypeDto.TRANSFER))
            .isEqualTo(ActivityFilterByItemTypeDto.TRANSFER)
    }

    @Test
    fun `eth activity type as collection activity type`() {
        assertThat(EthActivityConverter.asCollectionActivityType(ActivityTypeDto.BID))
            .isEqualTo(ActivityFilterByCollectionTypeDto.BID)

        assertThat(EthActivityConverter.asCollectionActivityType(ActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterByCollectionTypeDto.BURN)

        assertThat(EthActivityConverter.asCollectionActivityType(ActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterByCollectionTypeDto.LIST)

        assertThat(EthActivityConverter.asCollectionActivityType(ActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterByCollectionTypeDto.MINT)

        assertThat(EthActivityConverter.asCollectionActivityType(ActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterByCollectionTypeDto.MATCH)

        assertThat(EthActivityConverter.asCollectionActivityType(ActivityTypeDto.TRANSFER))
            .isEqualTo(ActivityFilterByCollectionTypeDto.TRANSFER)
    }

    @Test
    fun `eth activity type as global activity type`() {
        assertThat(EthActivityConverter.asGlobalActivityType(ActivityTypeDto.BID))
            .isEqualTo(ActivityFilterAllTypeDto.BID)

        assertThat(EthActivityConverter.asGlobalActivityType(ActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterAllTypeDto.BURN)

        assertThat(EthActivityConverter.asGlobalActivityType(ActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterAllTypeDto.LIST)

        assertThat(EthActivityConverter.asGlobalActivityType(ActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterAllTypeDto.MINT)

        assertThat(EthActivityConverter.asGlobalActivityType(ActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterAllTypeDto.SELL)

        assertThat(EthActivityConverter.asGlobalActivityType(ActivityTypeDto.TRANSFER))
            .isEqualTo(ActivityFilterAllTypeDto.TRANSFER)
    }

    private fun assertMatchSide(
        dest: OrderActivityMatchSideDto,
        expected: com.rarible.protocol.dto.OrderActivityMatchSideDto
    ) {
        assertThat(dest.hash).isEqualTo(expected.hash.prefixed())
        assertThat(dest.maker.value).isEqualTo(expected.maker.prefixed())
    }
}