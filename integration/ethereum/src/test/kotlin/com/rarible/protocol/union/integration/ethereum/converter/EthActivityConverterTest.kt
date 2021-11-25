package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.ActivityFilterAllTypeDto
import com.rarible.protocol.dto.ActivityFilterByCollectionTypeDto
import com.rarible.protocol.dto.ActivityFilterByItemTypeDto
import com.rarible.protocol.dto.ActivityFilterByUserTypeDto
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
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OrderMatchSwapDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc1155
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemBurnActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMintActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemTransferActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderActivityCancelBid
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderActivityCancelList
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderActivityMatch
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderBidActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderListActivity
import com.rarible.protocol.union.test.mock.CurrencyMock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthActivityConverterTest {

    private val ethActivityConverter = EthActivityConverter(CurrencyMock.currencyServiceMock)

    @Test
    fun `eth order activity match side - swap`() = runBlocking<Unit> {
        val dto = randomEthOrderActivityMatch()
        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as OrderMatchActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)

        assertThat(converted).isInstanceOf(OrderMatchSwapDto::class.java)
        converted as OrderMatchSwapDto
        assertMatchSide(converted.left, dto.left)
        assertMatchSide(converted.right, dto.right)

        assertThat(converted.source.name).isEqualTo(dto.source.name)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `eth order activity match side - nft to payment`() = runBlocking<Unit> {
        val swapDto = randomEthOrderActivityMatch()
        val left = swapDto.left.copy(asset = randomEthAssetErc1155())
        val dto = swapDto.copy(
            left = left
        )

        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as OrderMatchActivityDto

        assertThat(converted).isInstanceOf(OrderMatchSellDto::class.java)
        converted as OrderMatchSellDto

        assertThat(converted.type).isEqualTo(OrderMatchSellDto.Type.SELL)
        assertThat(converted.nft).isEqualTo(EthConverter.convert(left.asset, BlockchainDto.ETHEREUM))
        assertThat(converted.payment).isEqualTo(EthConverter.convert(swapDto.right.asset, BlockchainDto.ETHEREUM))
        assertThat(converted.seller).isEqualTo(EthConverter.convert(left.maker, BlockchainDto.ETHEREUM))
        assertThat(converted.buyer).isEqualTo(
            EthConverter.convert(
                swapDto.right.maker,
                BlockchainDto.ETHEREUM
            )
        )
        assertThat(converted.sellerOrderHash).isEqualTo(EthConverter.convert(swapDto.left.hash))
        assertThat(converted.buyerOrderHash).isEqualTo(EthConverter.convert(swapDto.right.hash))
        assertThat(converted.price).isEqualTo(swapDto.price)
        assertThat(converted.priceUsd).isEqualTo(swapDto.priceUsd)
        assertThat(converted.amountUsd).isEqualTo(swapDto.priceUsd!!.multiply(left.asset.valueDecimal))
    }

    @Test
    fun `eth order activity match side - payment to nft`() = runBlocking<Unit> {
        val swapDto = randomEthOrderActivityMatch()
        val right = swapDto.right.copy(asset = randomEthAssetErc1155())
        val dto = swapDto.copy(
            right = right
        )

        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as OrderMatchActivityDto

        assertThat(converted).isInstanceOf(OrderMatchSellDto::class.java)
        converted as OrderMatchSellDto

        assertThat(converted.type).isEqualTo(OrderMatchSellDto.Type.SELL)
        assertThat(converted.nft).isEqualTo(EthConverter.convert(right.asset, BlockchainDto.ETHEREUM))
        assertThat(converted.payment).isEqualTo(EthConverter.convert(swapDto.left.asset, BlockchainDto.ETHEREUM))
        assertThat(converted.seller).isEqualTo(EthConverter.convert(right.maker, BlockchainDto.ETHEREUM))
        assertThat(converted.buyer).isEqualTo(EthConverter.convert(swapDto.left.maker, BlockchainDto.ETHEREUM))
        assertThat(converted.sellerOrderHash).isEqualTo(EthConverter.convert(swapDto.right.hash))
        assertThat(converted.buyerOrderHash).isEqualTo(EthConverter.convert(swapDto.left.hash))
        assertThat(converted.price).isEqualTo(swapDto.price)
        assertThat(converted.priceUsd).isEqualTo(swapDto.priceUsd)
        assertThat(converted.amountUsd).isEqualTo(swapDto.priceUsd!!.multiply(right.asset.valueDecimal))
    }

    @Test
    fun `eth order activity bid`() = runBlocking<Unit> {
        val dto = randomEthOrderBidActivity()
        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as OrderBidActivityDto

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
    fun `eth order activity list`() = runBlocking<Unit> {
        val dto = randomEthOrderListActivity()
        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as OrderListActivityDto

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
    fun `eth order activity cancel bid`() = runBlocking<Unit> {
        val dto = randomEthOrderActivityCancelBid()
        val converted =
            ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as OrderCancelBidActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.source?.name).isEqualTo(dto.source.name)
        assertThat(converted.hash).isEqualTo(dto.hash.prefixed())
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `eth order activity cancel list`() = runBlocking<Unit> {
        val dto = randomEthOrderActivityCancelList()
        val converted =
            ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as OrderCancelListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.source?.name).isEqualTo(dto.source.name)
        assertThat(converted.hash).isEqualTo(dto.hash.prefixed())
        assertThat(converted.maker.value).isEqualTo(dto.maker.prefixed())
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo?.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo?.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo?.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo?.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `eth item activity mint`() = runBlocking<Unit> {
        val dto = randomEthItemMintActivity()
        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as MintActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owner.value).isEqualTo(dto.owner.prefixed())
        assertThat(converted.contract.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `eth item activity burn`() = runBlocking<Unit> {
        val dto = randomEthItemBurnActivity()
        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as BurnActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owner.value).isEqualTo(dto.owner.prefixed())
        assertThat(converted.contract.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `eth item activity transfer`() = runBlocking<Unit> {
        val dto = randomEthItemTransferActivity()
        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as TransferActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owner.value).isEqualTo(dto.owner.prefixed())
        assertThat(converted.contract.value).isEqualTo(dto.contract.prefixed())
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `eth activity type as user activity type`() {
        assertThat(ethActivityConverter.asUserActivityType(UserActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterByUserTypeDto.BURN)

        assertThat(ethActivityConverter.asUserActivityType(UserActivityTypeDto.BUY))
            .isEqualTo(ActivityFilterByUserTypeDto.BUY)

        assertThat(ethActivityConverter.asUserActivityType(UserActivityTypeDto.GET_BID))
            .isEqualTo(ActivityFilterByUserTypeDto.GET_BID)

        assertThat(ethActivityConverter.asUserActivityType(UserActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterByUserTypeDto.LIST)

        assertThat(ethActivityConverter.asUserActivityType(UserActivityTypeDto.CANCEL_LIST))
            .isEqualTo(ActivityFilterByUserTypeDto.CANCEL_LIST)

        assertThat(ethActivityConverter.asUserActivityType(UserActivityTypeDto.MAKE_BID))
            .isEqualTo(ActivityFilterByUserTypeDto.MAKE_BID)

        assertThat(ethActivityConverter.asUserActivityType(UserActivityTypeDto.CANCEL_BID))
            .isEqualTo(ActivityFilterByUserTypeDto.CANCEL_BID)

        assertThat(ethActivityConverter.asUserActivityType(UserActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterByUserTypeDto.MINT)

        assertThat(ethActivityConverter.asUserActivityType(UserActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterByUserTypeDto.SELL)

        assertThat(ethActivityConverter.asUserActivityType(UserActivityTypeDto.TRANSFER_FROM))
            .isEqualTo(ActivityFilterByUserTypeDto.TRANSFER_FROM)

        assertThat(ethActivityConverter.asUserActivityType(UserActivityTypeDto.TRANSFER_TO))
            .isEqualTo(ActivityFilterByUserTypeDto.TRANSFER_TO)
    }

    @Test
    fun `eth activity type as item activity type`() {
        assertThat(ethActivityConverter.asItemActivityType(ActivityTypeDto.BID))
            .isEqualTo(ActivityFilterByItemTypeDto.BID)

        assertThat(ethActivityConverter.asItemActivityType(ActivityTypeDto.CANCEL_BID))
            .isEqualTo(ActivityFilterByItemTypeDto.CANCEL_BID)

        assertThat(ethActivityConverter.asItemActivityType(ActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterByItemTypeDto.BURN)

        assertThat(ethActivityConverter.asItemActivityType(ActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterByItemTypeDto.LIST)

        assertThat(ethActivityConverter.asItemActivityType(ActivityTypeDto.CANCEL_LIST))
            .isEqualTo(ActivityFilterByItemTypeDto.CANCEL_LIST)

        assertThat(ethActivityConverter.asItemActivityType(ActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterByItemTypeDto.MINT)

        assertThat(ethActivityConverter.asItemActivityType(ActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterByItemTypeDto.MATCH)

        assertThat(ethActivityConverter.asItemActivityType(ActivityTypeDto.TRANSFER))
            .isEqualTo(ActivityFilterByItemTypeDto.TRANSFER)
    }

    @Test
    fun `eth activity type as collection activity type`() {
        assertThat(ethActivityConverter.asCollectionActivityType(ActivityTypeDto.BID))
            .isEqualTo(ActivityFilterByCollectionTypeDto.BID)

        assertThat(ethActivityConverter.asCollectionActivityType(ActivityTypeDto.CANCEL_BID))
            .isEqualTo(ActivityFilterByCollectionTypeDto.CANCEL_BID)

        assertThat(ethActivityConverter.asCollectionActivityType(ActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterByCollectionTypeDto.BURN)

        assertThat(ethActivityConverter.asCollectionActivityType(ActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterByCollectionTypeDto.LIST)

        assertThat(ethActivityConverter.asCollectionActivityType(ActivityTypeDto.CANCEL_LIST))
            .isEqualTo(ActivityFilterByCollectionTypeDto.CANCEL_LIST)

        assertThat(ethActivityConverter.asCollectionActivityType(ActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterByCollectionTypeDto.MINT)

        assertThat(ethActivityConverter.asCollectionActivityType(ActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterByCollectionTypeDto.MATCH)

        assertThat(ethActivityConverter.asCollectionActivityType(ActivityTypeDto.TRANSFER))
            .isEqualTo(ActivityFilterByCollectionTypeDto.TRANSFER)
    }

    @Test
    fun `eth activity type as global activity type`() {
        assertThat(ethActivityConverter.asGlobalActivityType(ActivityTypeDto.BID))
            .isEqualTo(ActivityFilterAllTypeDto.BID)

        assertThat(ethActivityConverter.asGlobalActivityType(ActivityTypeDto.CANCEL_BID))
            .isEqualTo(ActivityFilterAllTypeDto.CANCEL_BID)

        assertThat(ethActivityConverter.asGlobalActivityType(ActivityTypeDto.BURN))
            .isEqualTo(ActivityFilterAllTypeDto.BURN)

        assertThat(ethActivityConverter.asGlobalActivityType(ActivityTypeDto.LIST))
            .isEqualTo(ActivityFilterAllTypeDto.LIST)

        assertThat(ethActivityConverter.asGlobalActivityType(ActivityTypeDto.CANCEL_LIST))
            .isEqualTo(ActivityFilterAllTypeDto.CANCEL_LIST)

        assertThat(ethActivityConverter.asGlobalActivityType(ActivityTypeDto.MINT))
            .isEqualTo(ActivityFilterAllTypeDto.MINT)

        assertThat(ethActivityConverter.asGlobalActivityType(ActivityTypeDto.SELL))
            .isEqualTo(ActivityFilterAllTypeDto.SELL)

        assertThat(ethActivityConverter.asGlobalActivityType(ActivityTypeDto.TRANSFER))
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
