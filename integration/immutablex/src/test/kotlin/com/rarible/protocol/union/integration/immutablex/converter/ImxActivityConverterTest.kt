package com.rarible.protocol.union.integration.immutablex.converter

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.test.data.randomLong
import com.rarible.protocol.union.core.model.itemId
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.integration.data.randomImxOrder
import com.rarible.protocol.union.integration.data.randomImxTrade
import com.rarible.protocol.union.integration.data.randomImxTradeSide
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTokenEvent
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.stream.Stream

class ImxActivityConverterTest {

    @Test
    fun `convert trade - sell`() {
        val sell = randomImxOrder()
        val buy = sell.copy(orderId = randomLong(), buy = sell.sell, sell = sell.buy)

        // Sell - NFT
        val sellToken = sell.sell.data.tokenAddress!!
        val sellTokenId = sell.sell.data.tokenId!!

        // Buy - ERC20
        val buyToken = buy.sell.data.tokenAddress!!
        val buyTokenType = buy.sell.type

        // In Trade - make == sell (ERC721), take == buy (ERC20)
        val trade = randomImxTrade().copy(
            make = randomImxTradeSide(orderId = sell.orderId, token = sellToken, tokenId = sellTokenId),
            take = randomImxTradeSide(orderId = buy.orderId, token = null, tokenId = null, tokenType = buyTokenType),
        )

        val orders = mapOf(sell.orderId to sell, buy.orderId to buy)
        val result = ImxActivityConverter.convert(trade, orders, BlockchainDto.IMMUTABLEX) as OrderMatchSellDto

        assertThat(result.id).isEqualTo(trade.activityId)
        assertThat(result.type).isEqualTo(OrderMatchSellDto.Type.SELL) // TODO always sell?
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

    @ParameterizedTest
    @MethodSource("data")
    internal fun `should convert simple token event`(source: ImmutablexTokenEvent) {
        runBlocking {
            val activity = ImxActivityConverter.convert(source, emptyMap())
            assertThat(activity).isNotNull
            assertThat(activity.id).isEqualTo(source.activityId)
            assertThat(activity.date).isEqualTo(source.timestamp)
            assertThat(activity.lastUpdatedAt).isNull()
            assertThat("${activity.itemId()}").isEqualTo("IMMUTABLEX:${source.itemId()}")
        }
    }

    companion object {

        @JvmStatic
        fun data(): Stream<Arguments> {
            val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            val resourcePath = "/com/rarible/protocol/union/integration/immutablex/service/"
            return Stream.of(
                Arguments.of(
                    ImxActivityConverterTest::class.java.getResourceAsStream("${resourcePath}mint.json").use {
                        mapper.readValue(it!!, ImmutablexMint::class.java)
                    }
                ),
                Arguments.of(
                    ImxActivityConverterTest::class.java.getResourceAsStream("${resourcePath}transfer.json").use {
                        mapper.readValue(it!!, ImmutablexTransfer::class.java)
                    }
                )
            )
        }
    }
}
