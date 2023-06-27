package com.rarible.protocol.union.api.websocket

import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.model.UnionEthErc721AssetType
import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.core.test.WaitAssert
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemSubscriptionEventDto
import com.rarible.protocol.union.dto.ItemSubscriptionRequestDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.OrderSubscriptionEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.dto.OrdersByItemSubscriptionRequestDto
import com.rarible.protocol.union.dto.SubscriptionActionDto
import com.rarible.protocol.union.dto.SubscriptionEventDto
import com.rarible.protocol.union.dto.SubscriptionRequestDto
import com.rarible.protocol.union.enrichment.converter.OrderDtoConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Sinks
import java.math.BigInteger
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@IntegrationTest
internal class SubscriptionEventFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Autowired
    protected lateinit var webSocketEventsQueue: LinkedBlockingQueue<SubscriptionEventDto>

    @Autowired
    protected lateinit var webSocketRequests: Sinks.Many<List<SubscriptionRequestDto>>

    @Test
    fun `item event websocket test`() = runBlocking {
        val itemId = ItemIdDto(BlockchainDto.ETHEREUM, randomAddress().prefixed(), randomBigInt())

        val itemEventDto = ItemUpdateEventDto(
            itemId,
            "eventId",
            stubEventMark().toDto(),
            ItemDto(
                id = itemId,
                blockchain = BlockchainDto.ETHEREUM,
                lazySupply = BigInteger.ONE,
                pending = emptyList(),
                mintedAt = nowMillis(),
                lastUpdatedAt = nowMillis(),
                supply = BigInteger.ONE,
                deleted = false,
                auctions = emptyList(),
                sellers = 1
            ),
        )

        webSocketRequests.tryEmitNext(listOf(ItemSubscriptionRequestDto(SubscriptionActionDto.SUBSCRIBE, itemId)))

        delay(1000)
        webSocketEventsQueue.clear()

        itemProducer.send(
            KafkaMessage(
                key = itemId.value,
                value = itemEventDto
            )
        ).ensureSuccess()

        WaitAssert.wait {
            val event = withContext(Dispatchers.IO) {
                webSocketEventsQueue.poll(5, TimeUnit.SECONDS)
            }
            assertThat(event).isNotNull
            assertThat(event).isInstanceOf(ItemSubscriptionEventDto::class.java)
        }
    }

    @Test
    fun `order event websocket test by itemId`() = runBlocking {
        val order = ethOrderConverter.convert(randomEthV2OrderDto(), BlockchainDto.ETHEREUM)
        val type = (order.make.type as UnionEthErc721AssetType)
        val itemId = ItemIdDto(BlockchainDto.ETHEREUM, type.contract.value, type.tokenId)
        println("order is $order")
        println("itemId is $itemId")
        val dto = OrderDtoConverter.convert(order)

        val orderEventDto = OrderUpdateEventDto(order.id, "eventId", stubEventMark().toDto(), dto)

        webSocketRequests.tryEmitNext(
            listOf(
                OrdersByItemSubscriptionRequestDto(
                    SubscriptionActionDto.SUBSCRIBE,
                    itemId
                )
            )
        )

        delay(1000)
        webSocketEventsQueue.clear()

        val kafkaMessage = KafkaMessage(
            key = itemId.value,
            value = orderEventDto
        )
        orderProducer.send(kafkaMessage).ensureSuccess()

        WaitAssert.wait {
            val event = withContext(Dispatchers.IO) {
                webSocketEventsQueue.poll(5, TimeUnit.SECONDS)
            }
            assertThat(event).isNotNull
            assertThat(event).isInstanceOf(OrderSubscriptionEventDto::class.java)
        }
    }
}
