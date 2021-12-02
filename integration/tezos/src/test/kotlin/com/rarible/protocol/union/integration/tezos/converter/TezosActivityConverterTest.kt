package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.tezos.dto.BurnDto
import com.rarible.protocol.tezos.dto.MintDto
import com.rarible.protocol.tezos.dto.NftActivityFilterAllTypeDto
import com.rarible.protocol.tezos.dto.NftActivityFilterUserTypeDto
import com.rarible.protocol.tezos.dto.OrderActivityBidDto
import com.rarible.protocol.tezos.dto.OrderActivityCancelBidDto
import com.rarible.protocol.tezos.dto.OrderActivityCancelListDto
import com.rarible.protocol.tezos.dto.OrderActivityFilterAllTypeDto
import com.rarible.protocol.tezos.dto.OrderActivityFilterUserTypeDto
import com.rarible.protocol.tezos.dto.OrderActivityListDto
import com.rarible.protocol.tezos.dto.OrderActivityMatchDto
import com.rarible.protocol.tezos.dto.OrderActivityMatchTypeDto
import com.rarible.protocol.tezos.dto.OrderActivitySideMatchDto
import com.rarible.protocol.tezos.dto.TransferDto
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
import org.junit.jupiter.api.Test

class TezosActivityConverterTest {

    private val tezosActivityConverter = TezosActivityConverter(CurrencyMock.currencyServiceMock)

    @Test
    fun `tezos order activity match side - swap`() = runBlocking<Unit> {
        val actType = randomTezosOrderActivityMatch()
        val dto = actType.type as OrderActivityMatchDto
        val converted = tezosActivityConverter.convert(actType, BlockchainDto.TEZOS) as OrderMatchActivityDto

        assertThat(converted.id.value).isEqualTo(actType.id)
        assertThat(converted.date).isEqualTo(actType.date)
        assertThat(converted.source.name).isEqualTo(actType.source)

        assertThat(converted).isInstanceOf(OrderMatchSwapDto::class.java)
        converted as OrderMatchSwapDto
        assertMatchSide(converted.left, dto.left)
        assertMatchSide(converted.right, dto.right)

        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber.toLong())
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `tezos order activity match side - nft to payment`() = runBlocking<Unit> {
        val actDto = randomTezosOrderActivityMatch()
        val actType = (actDto.type as OrderActivityMatchDto)
        val left = actType.left.copy(asset = randomTezosAssetNFT())
        val actTypeNft = actType.copy(
            left = left,
            type = OrderActivityMatchTypeDto.ACCEPT_BID
        )
        val dto = actDto.copy(type = actTypeNft)

        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as OrderMatchActivityDto

        assertThat(converted).isInstanceOf(OrderMatchSellDto::class.java)
        converted as OrderMatchSellDto

        assertThat(converted.type).isEqualTo(OrderMatchSellDto.Type.ACCEPT_BID)
        assertThat(converted.nft).isEqualTo(TezosConverter.convert(left.asset, BlockchainDto.TEZOS))
        assertThat(converted.payment).isEqualTo(TezosConverter.convert(actTypeNft.right.asset, BlockchainDto.TEZOS))
        assertThat(converted.seller).isEqualTo(UnionAddressConverter.convert(BlockchainDto.TEZOS, left.maker))
        assertThat(converted.buyer).isEqualTo(
            UnionAddressConverter.convert(
                BlockchainDto.TEZOS,
                actTypeNft.right.maker
            )
        )
        assertThat(converted.sellerOrderHash).isEqualTo(actTypeNft.left.hash)
        assertThat(converted.buyerOrderHash).isEqualTo(actTypeNft.right.hash)
        assertThat(converted.price).isEqualTo(actTypeNft.price)
        // in tests all currencies == 1 usd
        assertThat(converted.priceUsd).isEqualTo(actTypeNft.price)
        assertThat(converted.amountUsd).isEqualTo(actTypeNft.price.multiply(left.asset.value))
    }

    @Test
    fun `tezos order activity match side - payment to nft`() = runBlocking<Unit> {
        val actDto = randomTezosOrderActivityMatch()
        val actType = (actDto.type as OrderActivityMatchDto)
        val right = actType.right.copy(asset = randomTezosAssetNFT())
        val actTypeNft = actType.copy(
            right = right
        )
        val dto = actDto.copy(type = actTypeNft)

        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as OrderMatchActivityDto

        assertThat(converted).isInstanceOf(OrderMatchSellDto::class.java)
        converted as OrderMatchSellDto

        assertThat(converted.type).isEqualTo(OrderMatchSellDto.Type.SELL)
        assertThat(converted.nft).isEqualTo(TezosConverter.convert(right.asset, BlockchainDto.TEZOS))
        assertThat(converted.payment).isEqualTo(TezosConverter.convert(actTypeNft.left.asset, BlockchainDto.TEZOS))
        assertThat(converted.seller).isEqualTo(UnionAddressConverter.convert(BlockchainDto.TEZOS, right.maker))
        assertThat(converted.buyer).isEqualTo(UnionAddressConverter.convert(BlockchainDto.TEZOS, actTypeNft.left.maker))
        assertThat(converted.sellerOrderHash).isEqualTo(actTypeNft.right.hash)
        assertThat(converted.buyerOrderHash).isEqualTo(actTypeNft.left.hash)
        assertThat(converted.price).isEqualTo(actTypeNft.price)
        // in tests all currencies == 1 usd
        assertThat(converted.priceUsd).isEqualTo(actTypeNft.price)
        assertThat(converted.amountUsd).isEqualTo(actTypeNft.price.multiply(right.asset.value))
    }

    @Test
    fun `tezos order activity bid`() = runBlocking<Unit> {
        val dto = randomTezosOrderBidActivity()
        val actType = dto.type as OrderActivityBidDto
        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as OrderBidActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.source?.name).isEqualTo(dto.source)

        assertThat(converted.price).isEqualTo(actType.price)
        // in tests all currencies == 1 usd
        assertThat(converted.priceUsd).isEqualTo(actType.price)
        assertThat(converted.take.value).isEqualTo(actType.take.value)
        assertThat(converted.make.value).isEqualTo(actType.make.value)
        assertThat(converted.maker.value).isEqualTo(actType.maker)
    }

    @Test
    fun `tezos order activity list`() = runBlocking<Unit> {
        val dto = randomTezosOrderListActivity()
        val actType = dto.type as OrderActivityListDto
        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as OrderListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.source?.name).isEqualTo(dto.source)

        assertThat(converted.price).isEqualTo(actType.price)
        // in tests all currencies == 1 usd
        assertThat(converted.priceUsd).isEqualTo(actType.price)
        assertThat(converted.take.value).isEqualTo(actType.take.value)
        assertThat(converted.make.value).isEqualTo(actType.make.value)
        assertThat(converted.maker.value).isEqualTo(actType.maker)
    }

    @Test
    fun `tezos order activity cancel bid`() = runBlocking<Unit> {
        val dto = randomTezosOrderActivityCancelBid()
        val actType = dto.type as OrderActivityCancelBidDto
        val converted =
            tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as OrderCancelBidActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.source?.name).isEqualTo(dto.source)

        assertThat(converted.hash).isEqualTo(actType.hash)
        assertThat(converted.maker.value).isEqualTo(actType.maker)
        assertThat(converted.transactionHash).isEqualTo(actType.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(actType.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(actType.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(actType.blockNumber.toLong())
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(actType.logIndex)
    }

    @Test
    fun `tezos order activity cancel list`() = runBlocking<Unit> {
        val dto = randomTezosOrderActivityCancelList()

        val actType = dto.type as OrderActivityCancelListDto

        val converted =
            tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as OrderCancelListActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.source?.name).isEqualTo(dto.source)

        assertThat(converted.hash).isEqualTo(actType.hash)
        assertThat(converted.maker.value).isEqualTo(actType.maker)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo?.transactionHash).isEqualTo(actType.transactionHash)
        assertThat(converted.blockchainInfo?.blockHash).isEqualTo(actType.blockHash)
        assertThat(converted.blockchainInfo?.blockNumber).isEqualTo(actType.blockNumber.toLong())
        assertThat(converted.blockchainInfo?.logIndex).isEqualTo(actType.logIndex)
    }

    @Test
    fun `tezos item activity mint`() = runBlocking<Unit> {
        val dto = randomTezosItemMintActivity()
        val actType = dto.type as MintDto
        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as MintActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)

        assertThat(converted.owner.value).isEqualTo(actType.owner)
        assertThat(converted.contract.value).isEqualTo(actType.contract)
        assertThat(converted.tokenId).isEqualTo(actType.tokenId)
        assertThat(converted.value).isEqualTo(actType.value.toBigInteger())
        assertThat(converted.transactionHash).isEqualTo(actType.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(actType.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(actType.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(actType.blockNumber.toLong())
    }

    @Test
    fun `tezos item activity burn`() = runBlocking<Unit> {
        val dto = randomTezosItemBurnActivity()
        val actType = dto.type as BurnDto
        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as BurnActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)

        assertThat(converted.owner.value).isEqualTo(actType.owner)
        assertThat(converted.contract.value).isEqualTo(actType.contract)
        assertThat(converted.tokenId).isEqualTo(actType.tokenId)
        assertThat(converted.value).isEqualTo(actType.value.toBigInteger())
        assertThat(converted.transactionHash).isEqualTo(actType.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(actType.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(actType.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(actType.blockNumber.toLong())
    }

    @Test
    fun `tezos item activity transfer`() = runBlocking<Unit> {
        val dto = randomTezosItemTransferActivity()
        val actType = dto.type as TransferDto
        val converted = tezosActivityConverter.convert(dto, BlockchainDto.TEZOS) as TransferActivityDto

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.date).isEqualTo(dto.date)

        assertThat(converted.owner.value).isEqualTo(actType.elt.owner)
        assertThat(converted.contract.value).isEqualTo(actType.elt.contract)
        assertThat(converted.tokenId).isEqualTo(actType.elt.tokenId)
        assertThat(converted.value).isEqualTo(actType.elt.value.toBigInteger())
        assertThat(converted.transactionHash).isEqualTo(actType.elt.transactionHash)
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(actType.elt.transactionHash)
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(actType.elt.blockHash)
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(actType.elt.blockNumber.toLong())
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

        assertThat(result).hasSize(5)
        assertThat(result).contains(
            OrderActivityFilterAllTypeDto.BID,
            OrderActivityFilterAllTypeDto.MATCH,
            OrderActivityFilterAllTypeDto.LIST,
            OrderActivityFilterAllTypeDto.CANCEL_LIST,
            OrderActivityFilterAllTypeDto.CANCEL_BID
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