package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.test.data.randomLong
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionBurnActivity
import com.rarible.protocol.union.core.model.UnionEthErc20AssetType
import com.rarible.protocol.union.core.model.UnionEthErc721AssetType
import com.rarible.protocol.union.core.model.UnionMintActivity
import com.rarible.protocol.union.core.model.UnionOrderMatchSell
import com.rarible.protocol.union.core.model.UnionOrderMatchSwap
import com.rarible.protocol.union.core.model.UnionTransferActivity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.integration.data.randomImxMint
import com.rarible.protocol.union.integration.data.randomImxOrder
import com.rarible.protocol.union.integration.data.randomImxTrade
import com.rarible.protocol.union.integration.data.randomImxTradeSide
import com.rarible.protocol.union.integration.data.randomImxTransfer
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigDecimal

class ImxActivityConverterTest {

    private val blockchain = BlockchainDto.IMMUTABLEX

    private val imxActivityConverter = ImxActivityConverter(
        mockk() {
            coEvery {
                toUsd(any(), any<UnionAssetType>(), any(), any())
            } returns BigDecimal.ONE
        },
        ImxOrderConverter()
    )

    @Test
    fun `convert trade - sell`() = runBlocking<Unit> {
        val sell = randomImxOrder()
        val buy = sell.copy(orderId = randomLong(), buy = sell.sell, sell = sell.buy)

        // Sell - NFT
        val sellToken = sell.sell.data.tokenAddress!!
        val sellTokenId = sell.sell.data.tokenId!!

        // Buy - ETH
        val buyTokenType = buy.sell.type

        // In Trade - make == sell (ERC721), take == buy (ETH)
        val trade = randomImxTrade().copy(
            make = randomImxTradeSide(orderId = sell.orderId, token = sellToken, tokenId = sellTokenId),
            take = randomImxTradeSide(orderId = buy.orderId, token = null, tokenId = null, tokenType = buyTokenType),
        )

        val orders = mapOf(sell.orderId to sell, buy.orderId to buy)
        val result = imxActivityConverter.convert(trade, orders, blockchain) as UnionOrderMatchSell

        assertThat(result.id).isEqualTo(trade.activityId)
        assertThat(result.source).isEqualTo(OrderActivitySourceDto.RARIBLE)
        assertThat(result.type).isEqualTo(UnionOrderMatchSell.Type.SELL) // TODO always sell?
        assertThat(result.date).isEqualTo(trade.timestamp)
        assertThat(result.transactionHash).isEqualTo(trade.transactionId.toString())

        assertThat(result.buyerOrderHash).isEqualTo(buy.orderId.toString())
        assertThat(result.sellerOrderHash).isEqualTo(sell.orderId.toString())

        assertThat(result.seller.value).isEqualTo(sell.creator)
        assertThat(result.buyer.value).isEqualTo(buy.creator)

        assertThat(result.nft.type).isInstanceOf(UnionEthErc721AssetType::class.java)
        assertThat(result.payment.type).isInstanceOf(UnionEthErc20AssetType::class.java)

        assertThat(result.nft.value).isEqualTo(BigDecimal.ONE)
        // Considering decimals = 0
        val expected = buy.sell.data.quantity!!.toBigDecimal()
        assertThat(result.payment.value).isEqualTo(expected)
        assertThat(result.price).isEqualTo(expected)
    }

    @Test
    fun `convert trade - swap`() = runBlocking<Unit> {
        // Both orders with ERC721 (we met such at Ropsten)
        val sell = randomImxOrder()
        val buy = randomImxOrder()

        // Sell - NFT
        val sellToken = sell.sell.data.tokenAddress!!
        val sellTokenId = sell.sell.data.tokenId!!

        // Buy - NFT
        val buyToken = buy.sell.data.tokenAddress!!
        val buyTokenId = buy.sell.data.tokenId!!

        // In Trade - make == sell (ERC721), take == buy (ERC20)
        val trade = randomImxTrade().copy(
            make = randomImxTradeSide(orderId = sell.orderId, token = sellToken, tokenId = sellTokenId),
            take = randomImxTradeSide(orderId = buy.orderId, token = buyToken, tokenId = buyTokenId),
        )

        val orders = mapOf(sell.orderId to sell, buy.orderId to buy)
        val result = imxActivityConverter.convert(trade, orders, blockchain) as UnionOrderMatchSwap

        assertThat(result.id).isEqualTo(trade.activityId)
        assertThat(result.source).isEqualTo(OrderActivitySourceDto.RARIBLE)
        assertThat(result.date).isEqualTo(trade.timestamp)

        assertThat(result.id).isEqualTo(trade.activityId)
        assertThat(result.transactionHash).isEqualTo(trade.transactionId.toString())

        assertThat(result.left.hash).isEqualTo(sell.orderId.toString())
        assertThat(result.right.hash).isEqualTo(buy.orderId.toString())

        assertThat(result.left.maker.value).isEqualTo(sell.creator)
        assertThat(result.right.maker.value).isEqualTo(buy.creator)
    }

    @Test
    fun `convert mint`() = runBlocking<Unit> {
        val imxMint = randomImxMint()

        val mint = imxActivityConverter.convert(imxMint, emptyMap(), blockchain) as UnionMintActivity

        assertThat(mint.id.value).isEqualTo(imxMint.transactionId.toString())
        assertThat(mint.date).isEqualTo(imxMint.timestamp)
        assertThat(mint.owner.value).isEqualTo(imxMint.user)
        assertThat(mint.itemId!!.value).isEqualTo(imxMint.encodedItemId())
        assertThat(mint.contract!!.value).isEqualTo(imxMint.token.data.tokenAddress)
        assertThat(mint.collection!!.value).isEqualTo(imxMint.token.data.tokenAddress)
        assertThat(mint.tokenId!!).isEqualTo(imxMint.token.data.encodedTokenId())
        assertThat(mint.value).isEqualTo(imxMint.token.data.quantity)
        assertThat(mint.transactionHash).isEqualTo(imxMint.transactionId.toString())
    }

    @Test
    fun `convert transfer`() = runBlocking<Unit> {
        val imxTransfer = randomImxTransfer()

        val transfer = imxActivityConverter.convert(imxTransfer, emptyMap(), blockchain) as UnionTransferActivity

        assertThat(transfer.id.value).isEqualTo(imxTransfer.transactionId.toString())
        assertThat(transfer.date).isEqualTo(imxTransfer.timestamp)
        assertThat(transfer.from.value).isEqualTo(imxTransfer.user)
        assertThat(transfer.owner.value).isEqualTo(imxTransfer.receiver)
        assertThat(transfer.itemId!!.value).isEqualTo(imxTransfer.encodedItemId())
        assertThat(transfer.contract!!.value).isEqualTo(imxTransfer.token.data.tokenAddress)
        assertThat(transfer.collection!!.value).isEqualTo(imxTransfer.token.data.tokenAddress)
        assertThat(transfer.tokenId!!).isEqualTo(imxTransfer.token.data.encodedTokenId())
        assertThat(transfer.value).isEqualTo(imxTransfer.token.data.quantity)
        assertThat(transfer.transactionHash).isEqualTo(imxTransfer.transactionId.toString())
    }

    @Test
    fun `convert transfer - burn`() = runBlocking<Unit> {
        val imxTransfer = randomImxTransfer(receiver = Address.ZERO().prefixed())

        val burn = imxActivityConverter.convert(imxTransfer, emptyMap(), blockchain) as UnionBurnActivity

        assertThat(burn.id.value).isEqualTo(imxTransfer.transactionId.toString())
        assertThat(burn.date).isEqualTo(imxTransfer.timestamp)
        assertThat(burn.owner.value).isEqualTo(imxTransfer.user)
        assertThat(burn.itemId!!.value).isEqualTo(imxTransfer.encodedItemId())
        assertThat(burn.contract!!.value).isEqualTo(imxTransfer.token.data.tokenAddress)
        assertThat(burn.collection!!.value).isEqualTo(imxTransfer.token.data.tokenAddress)
        assertThat(burn.tokenId!!).isEqualTo(imxTransfer.token.data.encodedTokenId())
        assertThat(burn.value).isEqualTo(imxTransfer.token.data.quantity)
        assertThat(burn.transactionHash).isEqualTo(imxTransfer.transactionId.toString())
    }
}
