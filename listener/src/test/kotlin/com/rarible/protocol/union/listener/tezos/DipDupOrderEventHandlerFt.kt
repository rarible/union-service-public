package com.rarible.protocol.union.listener.tezos

import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.KafkaMessage
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.client.core.model.OrderStatus
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaLoader
import com.rarible.protocol.union.integration.tezos.data.randomTezosTzktItemDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosTzktOwnershipDto
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.tzkt.model.Alias
import com.rarible.tzkt.model.Page
import com.rarible.tzkt.model.Token
import com.rarible.tzkt.model.TokenBalance
import com.rarible.tzkt.model.TokenInfo
import io.mockk.coEvery
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger
import java.time.ZoneOffset
import java.util.UUID

@IntegrationTest
class DipDupOrderEventHandlerFt : AbstractDipDupIntegrationTest() {

    @Autowired
    private lateinit var itemMetaLoader: ItemMetaLoader

    @Test
    @Disabled("Works locally, fix under PT-953")
    fun `should send dipdup order to outgoing topic`() = runWithKafka {

        // Order without item, we don't need to check Enrichment here
        val order = randomTezosOrderDto()
        val orderId = order.id

        val item = randomTezosTzktItemDto()

        val ownership = randomTezosTzktOwnershipDto()

        val token = token(item.contract!!.address)
        coEvery { tokenClient.token(any()) } returns token

        coEvery { ownershipClient.ownershipsByToken(any(), any(), any(), any()) } returns Page(emptyList(), null)
        coEvery { ownershipClient.ownershipById(any()) } returns tokenBalance()
        coEvery { itemMetaLoader.load(any()) } returns UnionMeta("test")

        dipDupOrderProducer.send(
            KafkaMessage(
                key = orderId,
                value = orderEvent(orderId)
            )
        ).ensureSuccess()

        waitAssert {
            val messages = findOrderUpdates(orderId)
            Assertions.assertThat(messages).hasSize(1)
        }
    }

    private fun token(contract: String): Token {
        return Token(
            id = 1,
            tokenId = "1",
            contract = Alias(
                address = contract
            ),
            balancesCount = 1,
            holdersCount = 1,
            transfersCount = 1,
            totalSupply = "1",
            firstTime = nowMillis().atOffset(ZoneOffset.UTC),
            lastTime = nowMillis().atOffset(ZoneOffset.UTC)
        )
    }

    private fun tokenBalance(): TokenBalance {
        return TokenBalance(
            id = 1,
            account = Alias(
                address = "test1"
            ),
            token = TokenInfo(
                tokenId = "1",
                contract = Alias(
                    address = "test2"
                )
            ),
            balance = "1",
            firstLevel = 1,
            lastLevel = 1,
            transfersCount = 1,
            firstTime = nowMillis().atOffset(ZoneOffset.UTC),
            lastTime = nowMillis().atOffset(ZoneOffset.UTC)
        )
    }

    private fun orderEvent(orderId: String): DipDupOrder {
        return DipDupOrder(
            id = orderId,
            internalOrderId = "0",
            fill = BigDecimal.ZERO,
            platform = TezosPlatform.RARIBLE_V2,
            payouts = emptyList(),
            originFees = emptyList(),
            status = OrderStatus.ACTIVE,
            startAt = null,
            endedAt = null,
            endAt = null,
            lastUpdatedAt = nowMillis().atOffset(ZoneOffset.UTC),
            createdAt = nowMillis().atOffset(ZoneOffset.UTC),
            maker = UUID.randomUUID().toString(),
            make = Asset(
                assetType = Asset.NFT(
                    contract = UUID.randomUUID().toString(),
                    tokenId = BigInteger.ONE
                ),
                assetValue = BigDecimal.ONE
            ),
            makePrice = null,
            taker = null,
            take = Asset(
                assetType = Asset.XTZ(),
                assetValue = BigDecimal.ONE
            ),
            takePrice = null,
            cancelled = false,
            salt = BigInteger.ONE,
            legacyData = null
        )
    }
}
