package com.rarible.protocol.union.listener.tezos

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderListActivity
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.tzkt.utils.Tezos
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigInteger

@IntegrationTest
class DipDupActivityEventHandlerFt : AbstractDipDupIntegrationTest() {

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        coEvery { tokenClient.token(any()) } returns randomTzktToken()
    }

    @Test
    @Disabled("Works locally, fix under PT-953")
    fun `should send dipdup orderlist activity to outgoing topic`() = runWithKafka {

        val activity = randomTezosOrderListActivity()
        val activityId = activity.id

        dipDupActivityProducer.send(
            KafkaMessage(
                key = activityId,
                value = randomDipDupActivityOrderListEvent(activityId)
            )
        ).ensureSuccess()

        waitAssert {
            val messages = findActivityUpdates(activityId, OrderListActivityDto::class.java)
            Assertions.assertThat(messages).hasSize(1)
        }
    }

    @Test
    @Disabled("Works locally, fix under PT-953")
    fun `should send dipdup mint activity to outgoing topic`() = runWithKafka {

        val activity = randomDipDupActivityMintEvent()
        val activityId = activity.transferId
        val ownershipId = OwnershipIdDto(
            blockchain = BlockchainDto.TEZOS,
            contract = activity.contract,
            tokenId = activity.tokenId,
            owner = activity.owner
        )

        coEvery { tokenClient.token(ownershipId.getItemId().value) } returns randomTzktToken(activity.contract, activity.tokenId, BigInteger.ONE)
        coEvery { ownershipClient.ownershipById(ownershipId.value) } returns randomTzktTokenBalance(activity.contract, activity.tokenId, activity.owner)

        dipDupActivityProducer.send(
            KafkaMessage(
                key = activityId,
                value = activity
            )
        ).ensureSuccess()

        waitAssert {
            val activities = findActivityUpdates(activityId, MintActivityDto::class.java)
            Assertions.assertThat(activities).hasSize(1)

            val ownerships = findOwnershipUpdates(ownershipId.value)
            Assertions.assertThat(ownerships)
                .hasSizeGreaterThan(0) // We got 2 msg because the second msg is sent from enrichment

            val items = findItemUpdates(ownershipId.getItemId().value)
            Assertions.assertThat(items).hasSize(1)
        }
    }

    @Test
    @Disabled("Works locally, fix under PT-953")
    fun `should send dipdup transfer activity to outgoing topic`() = runWithKafka {

        val activity = randomDipDupActivityTransferEvent()
        val activityId = activity.transferId
        val ownershipIdFrom = OwnershipIdDto(
            blockchain = BlockchainDto.TEZOS,
            contract = activity.contract,
            tokenId = activity.tokenId,
            owner = activity.from
        )
        val ownershipIdTo = OwnershipIdDto(
            blockchain = BlockchainDto.TEZOS,
            contract = activity.contract,
            tokenId = activity.tokenId,
            owner = activity.owner
        )

        coEvery { ownershipClient.ownershipById(ownershipIdFrom.value) } returns randomTzktTokenBalance(activity.contract, activity.tokenId, activity.from)
        coEvery { ownershipClient.ownershipById(ownershipIdTo.value) } returns randomTzktTokenBalance(activity.contract, activity.tokenId, activity.owner)

        dipDupActivityProducer.send(
            KafkaMessage(
                key = activityId,
                value = activity
            )
        ).ensureSuccess()

        waitAssert {
            val activities = findActivityUpdates(activityId, TransferActivityDto::class.java)
            Assertions.assertThat(activities).hasSize(1)

            val ownershipsFrom = findOwnershipUpdates(ownershipIdFrom.value)
            Assertions.assertThat(ownershipsFrom).hasSize(1)

            val ownershipsTo = findOwnershipUpdates(ownershipIdTo.value)
            Assertions.assertThat(ownershipsTo).hasSize(1)
        }
    }

    @Test
    @Disabled("Works locally, fix under PT-953")
    fun `should send dipdup transfer burn activity to outgoing topic`() = runWithKafka {

        val activity = randomDipDupActivityTransferEvent().copy(owner = Tezos.BURN_ADDRESS)
        val activityId = activity.transferId
        val ownershipId = OwnershipIdDto(
            blockchain = BlockchainDto.TEZOS,
            contract = activity.contract,
            tokenId = activity.tokenId,
            owner = activity.owner
        )

        coEvery { tokenClient.token(ownershipId.getItemId().value) } returns randomTzktToken(activity.contract, activity.tokenId, BigInteger.ZERO)
        coEvery { ownershipClient.ownershipById(ownershipId.value) } returns randomTzktTokenBalance(activity.contract, activity.tokenId, activity.owner)

        dipDupActivityProducer.send(
            KafkaMessage(
                key = activityId,
                value = activity
            )
        ).ensureSuccess()

        waitAssert {
            val activities = findActivityUpdates(activityId, BurnActivityDto::class.java)
            Assertions.assertThat(activities).hasSize(1)

            val ownerships = findOwnershipUpdates(ownershipId.value)
            Assertions.assertThat(ownerships).hasSize(1)

            val items = findItemDeletions(ownershipId.getItemId().value)
            Assertions.assertThat(items).hasSize(1)
        }
    }

    @Test
    @Disabled("Works locally, fix under PT-953")
    fun `should send dipdup burn activity to outgoing topic`() = runWithKafka {

        val activity = randomDipDupActivityBurnEvent()
        val activityId = activity.transferId
        val ownershipId = OwnershipIdDto(
            blockchain = BlockchainDto.TEZOS,
            contract = activity.contract,
            tokenId = activity.tokenId,
            owner = activity.owner
        )

        coEvery { tokenClient.token(ownershipId.getItemId().value) } returns randomTzktToken(activity.contract, activity.tokenId, BigInteger.ZERO)
        coEvery { ownershipClient.ownershipById(ownershipId.value) } returns randomTzktTokenBalance(activity.contract, activity.tokenId, activity.owner)

        dipDupActivityProducer.send(
            KafkaMessage(
                key = activityId,
                value = activity
            )
        ).ensureSuccess()

        waitAssert {
            val activities = findActivityUpdates(activityId, BurnActivityDto::class.java)
            Assertions.assertThat(activities).hasSize(1)

            val ownerships = findOwnershipUpdates(ownershipId.value)
            Assertions.assertThat(ownerships).hasSize(1)

            val items = findItemDeletions(ownershipId.getItemId().value)
            Assertions.assertThat(items).hasSize(1)
        }
    }
}
