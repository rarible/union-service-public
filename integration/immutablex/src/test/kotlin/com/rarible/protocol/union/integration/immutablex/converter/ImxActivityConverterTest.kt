package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.test.data.randomLong
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OrderMatchSwapDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.integration.data.randomImxMint
import com.rarible.protocol.union.integration.data.randomImxOrder
import com.rarible.protocol.union.integration.data.randomImxTrade
import com.rarible.protocol.union.integration.data.randomImxTradeSide
import com.rarible.protocol.union.integration.data.randomImxTransfer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigDecimal

class ImxActivityConverterTest {

    private val blockchain = BlockchainDto.IMMUTABLEX

    @Test
    fun `convert trade - sell`() {
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
        val result = ImxActivityConverter.convert(trade, orders, blockchain) as OrderMatchSellDto

        assertThat(result.id).isEqualTo(trade.activityId)
        assertThat(result.source).isEqualTo(OrderActivitySourceDto.RARIBLE)
        assertThat(result.type).isEqualTo(OrderMatchSellDto.Type.SELL) // TODO always sell?
        assertThat(result.date).isEqualTo(trade.timestamp)
        assertThat(result.transactionHash).isEqualTo(trade.transactionId.toString())

        assertThat(result.buyerOrderHash).isEqualTo(buy.orderId.toString())
        assertThat(result.sellerOrderHash).isEqualTo(sell.orderId.toString())

        assertThat(result.seller.value).isEqualTo(sell.creator)
        assertThat(result.buyer.value).isEqualTo(buy.creator)

        assertThat(result.nft.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(result.payment.type).isInstanceOf(EthEthereumAssetTypeDto::class.java)

        assertThat(result.nft.value).isEqualTo(BigDecimal.ONE)
        // Considering decimals = 0
        assertThat(result.payment.value).isEqualTo(buy.sell.data.quantity!!.toBigDecimal())
        assertThat(result.price).isEqualTo(buy.sell.data.quantity!!.toBigDecimal())
    }

    @Test
    fun `convert trade - swap`() {
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
        val result = ImxActivityConverter.convert(trade, orders, blockchain) as OrderMatchSwapDto

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
    fun `convert mint`() {
        val imxMint = randomImxMint()

        val mint = ImxActivityConverter.convert(imxMint, emptyMap(), blockchain) as MintActivityDto

        assertThat(mint.id.value).isEqualTo(imxMint.transactionId.toString())
        assertThat(mint.date).isEqualTo(imxMint.timestamp)
        assertThat(mint.owner.value).isEqualTo(imxMint.user)
        assertThat(mint.itemId!!.value).isEqualTo(imxMint.encodedItemId())
        assertThat(mint.contract!!.value).isEqualTo(imxMint.token.data.tokenAddress)
        assertThat(mint.tokenId!!).isEqualTo(imxMint.token.data.encodedTokenId())
        assertThat(mint.value).isEqualTo(imxMint.token.data.quantity)
        assertThat(mint.transactionHash).isEqualTo(imxMint.transactionId.toString())
    }

    @Test
    fun `convert transfer`() {
        val imxTransfer = randomImxTransfer()

        val transfer = ImxActivityConverter.convert(imxTransfer, emptyMap(), blockchain) as TransferActivityDto

        assertThat(transfer.id.value).isEqualTo(imxTransfer.transactionId.toString())
        assertThat(transfer.date).isEqualTo(imxTransfer.timestamp)
        assertThat(transfer.from.value).isEqualTo(imxTransfer.user)
        assertThat(transfer.owner.value).isEqualTo(imxTransfer.receiver)
        assertThat(transfer.itemId!!.value).isEqualTo(imxTransfer.encodedItemId())
        assertThat(transfer.contract!!.value).isEqualTo(imxTransfer.token.data.tokenAddress)
        assertThat(transfer.tokenId!!).isEqualTo(imxTransfer.token.data.encodedTokenId())
        assertThat(transfer.value).isEqualTo(imxTransfer.token.data.quantity)
        assertThat(transfer.transactionHash).isEqualTo(imxTransfer.transactionId.toString())
    }

    @Test
    fun `convert transfer - burn`() {
        val imxTransfer = randomImxTransfer(receiver = Address.ZERO().prefixed())

        val burn = ImxActivityConverter.convert(imxTransfer, emptyMap(), blockchain) as BurnActivityDto

        assertThat(burn.id.value).isEqualTo(imxTransfer.transactionId.toString())
        assertThat(burn.date).isEqualTo(imxTransfer.timestamp)
        assertThat(burn.owner.value).isEqualTo(imxTransfer.user)
        assertThat(burn.itemId!!.value).isEqualTo(imxTransfer.encodedItemId())
        assertThat(burn.contract!!.value).isEqualTo(imxTransfer.token.data.tokenAddress)
        assertThat(burn.tokenId!!).isEqualTo(imxTransfer.token.data.encodedTokenId())
        assertThat(burn.value).isEqualTo(imxTransfer.token.data.quantity)
        assertThat(burn.transactionHash).isEqualTo(imxTransfer.transactionId.toString())
    }
}
