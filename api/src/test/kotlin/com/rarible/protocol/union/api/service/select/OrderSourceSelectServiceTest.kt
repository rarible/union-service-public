package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.elastic.OrderElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdsDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.service.query.order.OrderApiMergeService
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OrderSourceSelectServiceTest {

    @MockK
    private lateinit var featureFlagsProperties: FeatureFlagsProperties

    @MockK
    private lateinit var orderApiService: OrderApiMergeService

    @MockK
    private lateinit var orderElasticService: OrderElasticService

    @InjectMockKs
    private lateinit var service: OrderSourceSelectService

    private val blockchains = listOf(mockk<BlockchainDto>())
    private val continuation = "some continuation"
    private val size = 42
    private val sort = OrderSortDto.LAST_UPDATE_ASC
    private val status = listOf(mockk<OrderStatusDto>())
    private val itemId = randomEthItemId()
    private val platform = mockk<PlatformDto>()
    private val maker = UnionAddress(BlockchainGroupDto.ETHEREUM, randomEthAddress())
    private val origin = "some origin"
    private val makers = listOf(UnionAddress(BlockchainGroupDto.ETHEREUM, randomEthAddress()))
    private val start = 500L
    private val end = 1000L

    private val elasticListResponse = listOf(mockk<OrderDto>())
    private val apiListResponse = listOf(mockk<OrderDto>())

    private val elasticOrdersResponse = mockk<OrdersDto>()
    private val apiOrdersResponse = mockk<OrdersDto>()

    @Nested
    inner class GetOrderByIdTest {

        @Test
        fun `should get order by id - select api even when elastic flag is on `() = runBlocking<Unit> {
            // given
            val id = "some id"
            val expected = mockk<OrderDto>()
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery { orderApiService.getOrderById(id) } returns expected

            // when
            val actual = service.getOrderById(id)

            // then
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `should get order by id - select api`() = runBlocking<Unit> {
            // given
            val id = "some id"
            val expected = mockk<OrderDto>()
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery { orderApiService.getOrderById(id) } returns expected

            // when
            val actual = service.getOrderById(id)

            // then
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    inner class GetByIdsTest {

        @Test
        fun `should get orders by ids - select api even when elastic flag is on `() = runBlocking<Unit> {
            // given
            val ids = mockk<OrderIdsDto>()
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery { orderApiService.getByIds(ids) } returns elasticListResponse

            // when
            val actual = service.getByIds(ids)

            // then
            assertThat(actual).isEqualTo(elasticListResponse)
        }

        @Test
        fun `should get orders by ids - select api`() = runBlocking<Unit> {
            // given
            val ids = mockk<OrderIdsDto>()
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery { orderApiService.getByIds(ids) } returns apiListResponse

            // when
            val actual = service.getByIds(ids)

            // then
            assertThat(actual).isEqualTo(apiListResponse)
        }
    }

    @Nested
    inner class GetOrdersAllTest {
        @ExperimentalStdlibApi
        @Test
        fun `should get all orders - select elastic`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery {
                orderElasticService.getOrdersAll(blockchains, continuation, size, sort, status)
            } returns elasticOrdersResponse

            // when
            val actual = service.getOrdersAll(blockchains, continuation, size, sort, status, null)

            // then
            assertThat(actual).isEqualTo(elasticOrdersResponse)
        }

        @Test
        fun `should get all orders - select api`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery {
                orderApiService.getOrdersAll(blockchains, continuation, size, sort, status)
            } returns apiOrdersResponse

            // when
            val actual = service.getOrdersAll(blockchains, continuation, size, sort, status, null)

            // then
            assertThat(actual).isEqualTo(apiOrdersResponse)
        }
    }

    @Nested
    inner class GetSellOrdersByItemTest {

        @Test
        fun `should get sell orders by item - select elastic`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery {
                orderElasticService.getSellOrdersByItem(itemId, platform, maker, origin, status, continuation, size)
            } returns elasticOrdersResponse

            // when
            val actual = service.getSellOrdersByItem(itemId, platform, maker, origin, status, continuation, size, null)

            // then
            assertThat(actual).isEqualTo(elasticOrdersResponse)
        }

        @Test
        fun `should get sell orders by item - select api`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery {
                orderApiService.getSellOrdersByItem(itemId, platform, maker, origin, status, continuation, size)
            } returns apiOrdersResponse

            // when
            val actual = service.getSellOrdersByItem(itemId, platform, maker, origin, status, continuation, size, null)

            // then
            assertThat(actual).isEqualTo(apiOrdersResponse)
        }
    }

    @Nested
    inner class GetOrderBidsByItemTest {

        @Test
        fun `should get order bids by item - select elastic`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery {
                orderElasticService.getOrderBidsByItem(itemId, platform, makers, origin, status, start, end, continuation, size)
            } returns elasticOrdersResponse

            // when
            val actual = service.getOrderBidsByItem(
                itemId, platform, makers, origin, status, start, end, continuation, size, null
            )

            // then
            assertThat(actual).isEqualTo(elasticOrdersResponse)
        }

        @Test
        fun `should get order bids by item - select api`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery {
                orderApiService.getOrderBidsByItem(itemId, platform, makers, origin, status, start, end, continuation, size)
            } returns apiOrdersResponse

            // when
            val actual = service.getOrderBidsByItem(
                itemId, platform, makers, origin, status, start, end, continuation, size, null
            )

            // then
            assertThat(actual).isEqualTo(apiOrdersResponse)
        }
    }

    @Nested
    inner class GetOrderBidsByMakerTest {

        @Test
        fun `should get order bids by maker - select elastic`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery {
                orderElasticService.getOrderBidsByMaker(blockchains, platform, listOf(maker), origin, status, start, end, continuation, size)
            } returns elasticOrdersResponse

            // when
            val actual = service.getOrderBidsByMaker(
                blockchains, platform, listOf(maker), origin, status, start, end, continuation, size, null
            )

            // then
            assertThat(actual).isEqualTo(elasticOrdersResponse)
        }

        @Test
        fun `should get order bids by maker - select api`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery {
                orderApiService.getOrderBidsByMaker(blockchains, platform, listOf(maker), origin, status, start, end, continuation, size)
            } returns apiOrdersResponse

            // when
            val actual = service.getOrderBidsByMaker(
                blockchains, platform, listOf(maker), origin, status, start, end, continuation, size, null
            )

            // then
            assertThat(actual).isEqualTo(apiOrdersResponse)
        }
    }

    @Nested
    inner class GetSellOrdersTest {

        @Test
        fun `should get sell orders - select elastic`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery {
                orderElasticService.getSellOrders(blockchains, platform, origin, continuation, size)
            } returns elasticOrdersResponse

            // when
            val actual = service.getSellOrders(blockchains, platform, origin, continuation, size, null)

            // then
            assertThat(actual).isEqualTo(elasticOrdersResponse)
        }

        @Test
        fun `should get sell orders - select api`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery {
                orderApiService.getSellOrders(blockchains, platform, origin, continuation, size)
            } returns apiOrdersResponse

            // when
            val actual = service.getSellOrders(blockchains, platform, origin, continuation, size, null)

            // then
            assertThat(actual).isEqualTo(apiOrdersResponse)
        }
    }

    @Nested
    inner class GetSellOrdersByMakerTest {
        @Test
        fun `should get sell orders by maker - select elastic`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery {
                orderElasticService.getSellOrdersByMaker(listOf(maker), blockchains, platform, origin, continuation, size, status)
            } returns elasticOrdersResponse

            // when
            val actual = service.getSellOrdersByMaker(
                listOf(maker), blockchains, platform, origin, continuation, size, status, null
            )

            // then
            assertThat(actual).isEqualTo(elasticOrdersResponse)
        }

        @Test
        fun `should get sell orders by maker - select api`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery {
                orderApiService.getSellOrdersByMaker(listOf(maker), blockchains, platform, origin, continuation, size, status)
            } returns apiOrdersResponse

            // when
            val actual = service.getSellOrdersByMaker(
                listOf(maker), blockchains, platform, origin, continuation, size, status, null
            )

            // then
            assertThat(actual).isEqualTo(apiOrdersResponse)
        }
    }
}
