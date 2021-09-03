package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.*
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.EthOrderBidActivityDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
class EthereumActivityEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `ethereum activity event`() = runWithKafka {

        val event: ActivityDto = OrderActivityBidDto(
            id = randomString(),
            date = nowMillis(),
            source = OrderActivityDto.Source.RARIBLE,
            hash = Word.apply(randomWord()),
            maker = randomAddress(),
            make = AssetDto(Erc20AssetTypeDto(randomAddress()), randomBigInt()),
            take = AssetDto(Erc20AssetTypeDto(randomAddress()), randomBigInt()),
            price = randomBigDecimal(),
            priceUsd = randomBigDecimal()
        )

        ethActivityProducer.send(message(event)).ensureSuccess()

        Wait.waitAssert {
            val messages = findActivityUpdates(event.id, EthOrderBidActivityDto::class.java)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].key).isEqualTo(event.id)
            Assertions.assertThat(messages[0].id).isEqualTo(event.id)
        }
    }

}