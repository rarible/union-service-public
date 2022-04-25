package com.rarible.protocol.union.listener.tezos

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.wait.Wait
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupOrderListActivity
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderListActivity
import com.rarible.protocol.union.listener.test.IntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

@IntegrationTest
class DipDupActivityEventHandlerFt : AbstractDipDupIntegrationTest() {

    @Test
    fun `should send dipdup activity to outgoing topic`() = runWithKafka {

        val activity = randomTezosOrderListActivity()
        val activityId = activity.id

        dipDupActivityProducer.send(
            KafkaMessage(
                key = activityId,
                value = activityEvent(activityId)
            )
        ).ensureSuccess()

        Wait.waitAssert {
            val messages = findActivityUpdates(activityId, OrderListActivityDto::class.java)
            Assertions.assertThat(messages).hasSize(1)
        }
    }

    private fun activityEvent(activityId: String): DipDupActivity {
        return DipDupOrderListActivity(
            id = activityId,
            date = Instant.now().atOffset(ZoneOffset.UTC),
            reverted = false,
            hash = "",
            maker = UUID.randomUUID().toString(),
            make = Asset(
                type = Asset.NFT(
                    contract = UUID.randomUUID().toString(),
                    tokenId = BigInteger.ONE
                ),
                value = BigDecimal.ONE
            ),
            take = Asset(
                type = Asset.XTZ(),
                value = BigDecimal.ONE
            ),
            price = BigDecimal.ONE,
            source = TezosPlatform.RARIBLE
        )
    }
}
