package com.rarible.protocol.union.worker.job.collection

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.converter.OrderDtoConverter
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import com.rarible.protocol.union.enrichment.test.data.randomUnionBidOrder
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import randomUnionAssetTypeErc20

@ExtendWith(MockKExtension::class)
class CustomCollectionOrderUpdaterTest {

    @MockK
    lateinit var orderService: OrderService

    @MockK
    lateinit var router: BlockchainRouter<OrderService>

    @MockK
    lateinit var producer: RaribleKafkaProducer<OrderEventDto>

    @MockK
    lateinit var enrichmentOrderService: EnrichmentOrderService

    @InjectMockKs
    lateinit var updater: CustomCollectionOrderUpdater

    @BeforeEach
    fun beforeEach() {
        clearMocks(router, enrichmentOrderService, orderService)
        every { router.getService(BlockchainDto.ETHEREUM) } returns orderService
        coEvery { enrichmentOrderService.enrich(emptyList()) } returns emptyList()
        coEvery { producer.send(any<Collection<KafkaMessage<OrderEventDto>>>()) } returns emptyFlow()
    }

    @Test
    fun `update - ok`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomUnionItem(itemId)

        val sellCurrency1 = randomUnionAssetTypeErc20(BlockchainDto.ETHEREUM)
        val sellCurrency2 = randomUnionAssetTypeErc20(BlockchainDto.ETHEREUM)
        val bidCurrency = randomUnionAssetTypeErc20(BlockchainDto.ETHEREUM)

        coEvery { orderService.getBidCurrencies(itemId.value) } returns listOf(bidCurrency)
        coEvery { orderService.getSellCurrencies(itemId.value) } returns listOf(sellCurrency1, sellCurrency2)

        val sellOrder1 = randomUnionSellOrder(itemId)
        val sellOrder2 = randomUnionSellOrder(itemId)

        val bidOrder1 = randomUnionBidOrder(itemId)
        val bidOrder2 = randomUnionBidOrder(itemId)

        mockkGetSellOrders(itemId, sellCurrency1.currencyId()!!, null, "1", sellOrder1)
        mockkGetSellOrders(itemId, sellCurrency1.currencyId()!!, "1", null)
        mockkGetSellOrders(itemId, sellCurrency2.currencyId()!!, null, null, sellOrder2)

        mockkGetBidOrders(itemId, bidCurrency.currencyId()!!, null, "1", bidOrder1)
        mockkGetBidOrders(itemId, bidCurrency.currencyId()!!, null, null, bidOrder2)

        val sellDto1 = OrderDtoConverter.convert(sellOrder1)
        val sellDto2 = OrderDtoConverter.convert(sellOrder2)
        val bidDto1 = OrderDtoConverter.convert(bidOrder1)
        val bidDto2 = OrderDtoConverter.convert(bidOrder2)

        coEvery { enrichmentOrderService.enrich(listOf(sellOrder1)) } returns listOf(sellDto1)
        coEvery { enrichmentOrderService.enrich(listOf(sellOrder2)) } returns listOf(sellDto2)
        coEvery { enrichmentOrderService.enrich(listOf(bidOrder1)) } returns listOf(bidDto1)
        coEvery { enrichmentOrderService.enrich(listOf(bidOrder2)) } returns listOf(bidDto2)

        updater.update(item)

        assertEvents(listOf(sellOrder1.id))
        assertEvents(listOf(bidOrder2.id))
        assertEvents(listOf(sellOrder1.id))
        assertEvents(listOf(bidOrder2.id))
    }

    private fun assertEvents(
        expected: List<OrderIdDto>
    ) {
        coVerify {
            producer.send(match<Collection<KafkaMessage<OrderEventDto>>> { events ->
                val received = events.map { it.value.orderId }
                assertThat(received).isEqualTo(expected)
                true
            })
        }
    }

    private fun mockkGetSellOrders(
        itemId: ItemIdDto,
        currencyId: String,
        continuation: String? = null,
        returnContinuation: String? = null,
        vararg orders: UnionOrder
    ) {

        coEvery {
            orderService.getSellOrdersByItem(
                platform = null,
                itemId = itemId.value,
                continuation = continuation,
                maker = null,
                origin = null,
                status = emptyList(),
                currencyId = currencyId,
                size = any()
            )
        } returns Slice(returnContinuation, orders.toList())
    }

    private fun mockkGetBidOrders(
        itemId: ItemIdDto,
        currencyId: String,
        continuation: String? = null,
        returnContinuation: String? = null,
        vararg orders: UnionOrder
    ) {

        coEvery {
            orderService.getOrderBidsByItem(
                platform = null,
                itemId = itemId.value,
                continuation = continuation,
                origin = null,
                status = emptyList(),
                makers = null,
                start = null,
                end = null,
                currencyAddress = currencyId,
                size = any()
            )
        } returns Slice(returnContinuation, orders.toList())
    }
}