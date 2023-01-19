package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.AuctionBidActivityDto
import com.rarible.protocol.union.dto.AuctionCancelActivityDto
import com.rarible.protocol.union.dto.AuctionEndActivityDto
import com.rarible.protocol.union.dto.AuctionFinishActivityDto
import com.rarible.protocol.union.dto.AuctionOpenActivityDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.ItemIdDto
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
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc1155
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionBidActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionCancelActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionEndActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionFinishActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionOpenActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionStartActivity
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

    private val ethAuctionConverter = EthAuctionConverter(CurrencyMock.currencyServiceMock)
    private val ethActivityConverter = EthActivityConverter(ethAuctionConverter)

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
        assertThat(converted.sellMarketplaceMarker).isEqualTo(swapDto.marketplaceMarker?.toString())
        assertThat(converted.buyMarketplaceMarker).isEqualTo(swapDto.counterMarketplaceMarker?.toString())
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
        assertThat(converted.contract!!.value).isEqualTo(dto.contract.prefixed()) // TODO remove later
        assertThat(converted.tokenId).isEqualTo(dto.tokenId) // TODO remove later
        assertThat(converted.itemId).isEqualTo(ItemIdDto(BlockchainDto.ETHEREUM, dto.contract.prefixed(), dto.tokenId))
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.mintPrice).isEqualTo(dto.mintPrice)
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
        assertThat(converted.contract!!.value).isEqualTo(dto.contract.prefixed()) // TODO remove later
        assertThat(converted.tokenId).isEqualTo(dto.tokenId) // TODO remove later
        assertThat(converted.itemId).isEqualTo(ItemIdDto(BlockchainDto.ETHEREUM, dto.contract.prefixed(), dto.tokenId))
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
        assertThat(converted.contract!!.value).isEqualTo(dto.contract.prefixed()) // TODO remove later
        assertThat(converted.tokenId).isEqualTo(dto.tokenId) // TODO remove later
        assertThat(converted.itemId).isEqualTo(ItemIdDto(BlockchainDto.ETHEREUM, dto.contract.prefixed(), dto.tokenId))
        assertThat(converted.value).isEqualTo(dto.value)
        assertThat(converted.purchase).isEqualTo(dto.purchase)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        // TODO UNION remove in 1.19
        assertThat(converted.blockchainInfo!!.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.blockchainInfo!!.blockHash).isEqualTo(dto.blockHash.prefixed())
        assertThat(converted.blockchainInfo!!.blockNumber).isEqualTo(dto.blockNumber)
        assertThat(converted.blockchainInfo!!.logIndex).isEqualTo(dto.logIndex)
    }

    @Test
    fun `eth auction open`() = runBlocking<Unit> {
        val dto = randomEthAuctionOpenActivity()

        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as AuctionOpenActivityDto

        assertThat(converted.id).isEqualTo(ActivityIdDto(BlockchainDto.ETHEREUM, dto.id))
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.auction).isEqualTo(ethAuctionConverter.convert(dto.auction, BlockchainDto.ETHEREUM))
    }

    @Test
    fun `eth auction cancelled`() = runBlocking<Unit> {
        val dto = randomEthAuctionCancelActivity()

        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as AuctionCancelActivityDto

        assertThat(converted.id).isEqualTo(ActivityIdDto(BlockchainDto.ETHEREUM, dto.id))
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.auction).isEqualTo(ethAuctionConverter.convert(dto.auction, BlockchainDto.ETHEREUM))
    }

    @Test
    fun `eth auction finished`() = runBlocking<Unit> {
        val dto = randomEthAuctionFinishActivity()

        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as AuctionFinishActivityDto

        assertThat(converted.id).isEqualTo(ActivityIdDto(BlockchainDto.ETHEREUM, dto.id))
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.auction).isEqualTo(ethAuctionConverter.convert(dto.auction, BlockchainDto.ETHEREUM))
    }

    @Test
    fun `eth auction started`() = runBlocking<Unit> {
        val dto = randomEthAuctionStartActivity()

        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as AuctionStartActivityDto

        assertThat(converted.id).isEqualTo(ActivityIdDto(BlockchainDto.ETHEREUM, dto.id))
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.auction).isEqualTo(ethAuctionConverter.convert(dto.auction, BlockchainDto.ETHEREUM))
    }

    @Test
    fun `eth auction ended`() = runBlocking<Unit> {
        val dto = randomEthAuctionEndActivity()

        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as AuctionEndActivityDto

        assertThat(converted.id).isEqualTo(ActivityIdDto(BlockchainDto.ETHEREUM, dto.id))
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.auction).isEqualTo(ethAuctionConverter.convert(dto.auction, BlockchainDto.ETHEREUM))
    }

    @Test
    fun `eth auction bid`() = runBlocking<Unit> {
        val dto = randomEthAuctionBidActivity()

        val converted = ethActivityConverter.convert(dto, BlockchainDto.ETHEREUM) as AuctionBidActivityDto

        assertThat(converted.id).isEqualTo(ActivityIdDto(BlockchainDto.ETHEREUM, dto.id))
        assertThat(converted.date).isEqualTo(dto.date)
        assertThat(converted.transactionHash).isEqualTo(dto.transactionHash.prefixed())
        assertThat(converted.bid.amount).isEqualTo(dto.bid.amount)
        assertThat(converted.bid.buyer.value).isEqualTo(dto.bid.buyer.prefixed())
        assertThat(converted.auction).isEqualTo(ethAuctionConverter.convert(dto.auction, BlockchainDto.ETHEREUM))
    }

    private fun assertMatchSide(
        dest: OrderActivityMatchSideDto,
        expected: com.rarible.protocol.dto.OrderActivityMatchSideDto
    ) {
        assertThat(dest.hash).isEqualTo(expected.hash.prefixed())
        assertThat(dest.maker.value).isEqualTo(expected.maker.prefixed())
    }
}
