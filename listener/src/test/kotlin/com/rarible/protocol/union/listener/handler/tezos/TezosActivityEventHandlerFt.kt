package com.rarible.protocol.union.listener.handler.tezos

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.tezos.dto.ActivityDto
import com.rarible.protocol.tezos.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomTezosItemMintActivity
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
class TezosActivityEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `tezos activity event`() = runWithKafka {

        val dto: ActivityTypeDto = randomTezosItemMintActivity()
        val event = ActivityDto(
            id = randomString(),
            date = null,
            source = null,
            type = dto
        )

        tezosActivityProducer.send(message(event)).ensureSuccess()

        val expectedKey = ActivityIdDto(
            blockchain = BlockchainDto.TEZOS,
            value = dto.id
        ).fullId()

        Wait.waitAssert {
            val messages = findActivityUpdates(dto.id, MintActivityDto::class.java)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(expectedKey)
            assertThat(messages[0].id).isEqualTo(expectedKey)
        }
    }

}