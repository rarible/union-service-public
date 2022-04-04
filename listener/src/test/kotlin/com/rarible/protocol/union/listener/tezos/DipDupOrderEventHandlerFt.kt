package com.rarible.protocol.union.listener.tezos

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.wait.Wait
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.client.core.model.OrderStatus
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.protocol.union.integration.tezos.data.randomTezosNftItemDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOwnershipDto
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

@IntegrationTest
class DipDupOrderEventHandlerFt : AbstractDipDupIntegrationTest() {

    @Test
    fun `should send dipdup order to outgoing topic`() = runWithKafka {

        // Order without item, we don't need to check Enrichment here
        val order = randomTezosOrderDto()
        val orderId = order.hash
        coEvery { testTezosOrderApi.getOrderByHash(any()) } returns order.toMono()

        val item = randomTezosNftItemDto()
        coEvery { testTezosItemApi.getNftItemById(any(), any()) } returns item.toMono()

        val ownership = randomTezosOwnershipDto()
        coEvery { testTezosOwnershipApi.getNftOwnershipById(any()) } returns ownership.toMono()

        dipDupOrderProducer.send(
            KafkaMessage(
                key = orderId,
                value = orderEvent(orderId)
            )
        ).ensureSuccess()

        Wait.waitAssert {
            val messages = findOrderUpdates(orderId)
            Assertions.assertThat(messages).hasSize(1)
        }
    }

    private fun orderEvent(orderId: String): DipDupOrder {
        return DipDupOrder(
            id = orderId,
            fill = BigDecimal.ZERO,
            platform = TezosPlatform.HEN,
            status = OrderStatus.ACTIVE,
            startedAt = null,
            endedAt = null,
            makeStock = BigDecimal.ONE,
            lastUpdatedAt = Instant.now().atOffset(ZoneOffset.UTC),
            createdAt = Instant.now().atOffset(ZoneOffset.UTC),
            makePrice = BigDecimal.ONE,
            maker = UUID.randomUUID().toString(),
            make = Asset(
                type = Asset.NFT(
                    contract = UUID.randomUUID().toString(),
                    tokenId = BigInteger.ONE
                ),
                value = BigDecimal.ONE
            ),
            taker = null,
            take = Asset(
                type = Asset.XTZ(),
                value = BigDecimal.ONE
            ),
            cancelled = false,
            salt = BigInteger.ONE
        )
    }
}
