package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.tezos.dto.NftActivityFilterAllTypeDto
import com.rarible.protocol.tezos.dto.NftActivityFilterUserTypeDto
import com.rarible.protocol.tezos.dto.OrderActivityFilterAllTypeDto
import com.rarible.protocol.tezos.dto.OrderActivityFilterUserTypeDto
import com.rarible.protocol.tezos.dto.OrderActivitySideMatchDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
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
import com.rarible.protocol.union.integration.tezos.data.randomTezosAssetNFT
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemBurnActivity
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemMintActivity
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemTransferActivity
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderActivityCancelBid
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderActivityCancelList
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderActivityMatch
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderBidActivity
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderListActivity
import com.rarible.protocol.union.test.mock.CurrencyMock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class TezosActivityConverterTest {

    private val tezosActivityConverter = TezosActivityConverter(CurrencyMock.currencyServiceMock)

    @Test
    fun `tezos order activity match side - swap`() = runBlocking<Unit> {
        val dto = randomTezosOrderActivityMatch()
        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as OrderMatchActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)

        assertThat(converted).isInstanceOf(OrderMatchSwapDto::class.java)
        converted as OrderMatchSwapDto
        assertMatchSide(converted.left, dto.left)
        assertMatchSide(converted.right, dto.right)

        assertThat(converted.source.name).isEqualTo(dto.source)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber.toLong())
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    @Disabled // TODO TEZOS enable when type field appears in activity
    fun `tezos order activity match side - nft to payment`() = runBlocking<Unit> {
        val swapDto = randomTezosOrderActivityMatch()
        val left = swapDto.left.copy(asset = randomTezosAssetNFT())
        val dto = swapDto.copy(
            left = left
        )

        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as OrderMatchActivityDto

        assertThat(converted).isInstanceOf(OrderMatchSellDto::class.java)
        converted as OrderMatchSellDto

        assertThat(converted.nft).isEqualTo(TezosConverter.convert(left.asset, BlockchainDto.TEZOS))
        assertThat(converted.payment).isEqualTo(TezosConverter.convert(swapDto.right.asset, BlockchainDto.TEZOS))
        assertThat(converted.seller).isEqualTo(UnionAddressConverter.convert(BlockchainDto.TEZOS, left.maker))
        assertThat(converted.buyer).isEqualTo(
            UnionAddressConverter.convert(
                BlockchainDto.TEZOS,
                swapDto.right.maker
            )
        )
        assertThat(converted.price).isEqualTo(swapDto.price)
        // in tests all currencies == 1 usd
        assertThat(converted.priceUsd).isEqualTo(swapDto.price)
        assertThat(converted.amountUsd).isEqualTo(swapDto.price.multiply(left.asset.value))
    }

    @Test
    @Disabled // TODO TEZOS enable when type field appears in activity
    fun `tezos order activity match side - payment to nft`() = runBlocking<Unit> {
        val swapDto = randomTezosOrderActivityMatch()
        val right = swapDto.right.copy(asset = randomTezosAssetNFT())
        val dto = swapDto.copy(
            right = right
        )

        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as OrderMatchActivityDto

        assertThat(converted).isInstanceOf(OrderMatchSellDto::class.java)
        converted as OrderMatchSellDto

        assertThat(converted.nft).isEqualTo(TezosConverter.convert(right.asset, BlockchainDto.TEZOS))
        assertThat(converted.payment).isEqualTo(TezosConverter.convert(swapDto.left.asset, BlockchainDto.TEZOS))
        assertThat(converted.seller).isEqualTo(UnionAddressConverter.convert(BlockchainDto.TEZOS, right.maker))
        assertThat(converted.buyer).isEqualTo(UnionAddressConverter.convert(BlockchainDto.TEZOS, swapDto.left.maker))
        assertThat(converted.price).isEqualTo(swapDto.price)
        // in tests all currencies == 1 usd
        assertThat(converted.priceUsd).isEqualTo(swapDto.price)
        assertThat(converted.amountUsd).isEqualTo(swapDto.price.multiply(right.asset.value))
    }

    @Test
    fun `tezos order activity bid`() = runBlocking<Unit> {
        val dto = randomTezosOrderBidActivity()
        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as OrderBidActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.price).isEqualTo(dto.price)
        // in tests all currencies == 1 usd
        assertThat(converted.priceUsd).isEqualTo(dto.price)
        assertThat(converted.source?.name).isEqualTo(dto.source)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
    }

    @Test
    fun `tezos order activity list`() = runBlocking<Unit> {
        val dto = randomTezosOrderListActivity()
        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as OrderListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.price).isEqualTo(dto.price)
        // in tests all currencies == 1 usd
        assertThat(converted.priceUsd).isEqualTo(dto.price)
        assertThat(converted.source?.name).isEqualTo(dto.source)
        assertThat(converted.take.value).isEqualTo(dto.take.value)
        assertThat(converted.make.value).isEqualTo(dto.make.value)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
    }

    @Test
    fun `tezos order activity cancel bid`() = runBlocking<Unit> {
        val dto = randomTezosOrderActivityCancelBid()
        val converted =
            tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as OrderCancelBidActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.source?.name).isEqualTo(dto.source)
        assertThat(converted.hash).isEqualTo(dto.hash)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber.toLong())
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `tezos order activity cancel list`() = runBlocking<Unit> {
        val dto = randomTezosOrderActivityCancelList()
        val converted =
            tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as OrderCancelListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.source?.name).isEqualTo(dto.source)
        assertThat(converted.hash).isEqualTo(dto.hash)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        assertThat(converted.blockchainInfo?.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo?.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo?.blockNumber).isEqualTo(dto.blockNumber.toLong())
        assertThat(converted.blockchainInfo?.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `tezos item activity mint`() = runBlocking<Unit> {
        val dto = randomTezosItemMintActivity()
        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as MintActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owner.value).isEqualTo(dto.owner)
        assertThat(converted.contract.value).isEqualTo(dto.contract)
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.value).isEqualTo(dto.value.toBigInteger())
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber.toLong())
    }

    @Test
    fun `tezos item activity burn`() = runBlocking<Unit> {
        val dto = randomTezosItemBurnActivity()
        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as BurnActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owner.value).isEqualTo(dto.owner)
        assertThat(converted.contract.value).isEqualTo(dto.contract)
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.value).isEqualTo(dto.value.toBigInteger())
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber.toLong())
    }

    @Test
    fun `tezos item activity transfer`() = runBlocking<Unit> {
        val dto = randomTezosItemTransferActivity()
        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as TransferActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.owner.value).isEqualTo(dto.elt.owner)
        assertThat(converted.contract.value).isEqualTo(dto.elt.contract)
        assertThat(converted.tokenId).isEqualTo(dto.elt.tokenId)
        assertThat(converted.value).isEqualTo(dto.elt.value.toBigInteger())
        assertThat(converted.transactionHash).isEqualTo(dto.elt.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.elt.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.elt.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.elt.blockNumber.toLong())
    }

    @Test
    fun `tezos nft activity types`() {
        val result = tezosActivityConverter.convertToNftTypes(
            // To check deduplication
            ActivityTypeDto.values().toList() + ActivityTypeDto.values().toList()
        )

        assertThat(result).hasSize(3)
        assertThat(result).contains(
            NftActivityFilterAllTypeDto.MINT,
            NftActivityFilterAllTypeDto.BURN,
            NftActivityFilterAllTypeDto.TRANSFER
        )
    }

    @Test
    fun `tezos order activity types`() {
        val result = tezosActivityConverter.convertToOrderTypes(
            // To check deduplication
            ActivityTypeDto.values().toList() + ActivityTypeDto.values().toList()
        )

        assertThat(result).hasSize(3)
        assertThat(result).contains(
            OrderActivityFilterAllTypeDto.BID,
            OrderActivityFilterAllTypeDto.MATCH,
            OrderActivityFilterAllTypeDto.LIST
        )
    }

    @Test
    fun `tezos nft user activity types`() {
        val result = tezosActivityConverter.convertToOrderUserTypes(
            // To check deduplication
            UserActivityTypeDto.values().toList() + UserActivityTypeDto.values().toList()
        )

        assertThat(result).hasSize(5)
        assertThat(result).contains(
            OrderActivityFilterUserTypeDto.SELL,
            OrderActivityFilterUserTypeDto.LIST,
            OrderActivityFilterUserTypeDto.GET_BID,
            OrderActivityFilterUserTypeDto.BUY,
            OrderActivityFilterUserTypeDto.MAKE_BID
        )
    }

    @Test
    fun `tezos order user activity types`() {
        val result = tezosActivityConverter.convertToNftUserTypes(
            // To check deduplication
            UserActivityTypeDto.values().toList() + UserActivityTypeDto.values().toList()
        )

        assertThat(result).hasSize(4)
        assertThat(result).contains(
            NftActivityFilterUserTypeDto.MINT,
            NftActivityFilterUserTypeDto.BURN,
            NftActivityFilterUserTypeDto.TRANSFER_TO,
            NftActivityFilterUserTypeDto.TRANSFER_FROM
        )
    }

    private fun assertMatchSide(
        dest: OrderActivityMatchSideDto,
        expected: OrderActivitySideMatchDto
    ) {
        assertThat(dest.hash).isEqualTo(expected.hash)
        assertThat(dest.maker.value).isEqualTo(expected.maker)
    }
}