package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.api.OrderApiService
import com.rarible.protocol.union.api.service.elastic.OrderElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import io.mockk.coEvery
import io.mockk.coVerify
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
    private lateinit var orderApiService: OrderApiService

    @MockK
    private lateinit var orderElasticService: OrderElasticService

    @InjectMockKs
    private lateinit var service: OrderSourceSelectService

    private val blockchains = listOf(mockk<BlockchainDto>())
    private val blockchain = mockk<BlockchainDto>()
    private val continuation = "some continuation"
    private val size = 42
    private val sort = OrderSortDto.LAST_UPDATE_ASC
    private val status = listOf(mockk<OrderStatusDto>())
    private val itemId = "some item id"
    private val platform = mockk<PlatformDto>()
    private val maker = "some maker"
    private val origin = "some origin"
    private val makers = listOf("another maker")
    private val start = 500L
    private val end = 1000L

    private val elasticListResponse = listOf(mockk<OrderDto>())
    private val apiListResponse = listOf(mockk<OrderDto>())

    private val elasticSliceResponse = mockk<Slice<OrderDto>>()
    private val apiSliceResponse = mockk<Slice<OrderDto>>()

    @Nested
    inner class GetByIdsTest {

        @Test
        fun `should get orders by ids - select elastic`() = runBlocking<Unit> {
            // given
            val ids = listOf<OrderIdDto>(mockk())
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery { orderElasticService.getByIds(ids) } returns elasticListResponse

            // when
            val actual = service.getByIds(ids)

            // then
            assertThat(actual).isEqualTo(elasticListResponse)
        }

        @Test
        fun `should get orders by ids - select api`() = runBlocking<Unit> {
            // given
            val ids = listOf<OrderIdDto>(mockk())
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
        @Test
        fun `should get all orders - select elastic`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery {
                orderElasticService.getOrdersAll(blockchains, continuation, size, sort, status)
            } returns elasticSliceResponse

            // when
            val actual = service.getOrdersAll(blockchains, continuation, size, sort, status)

            // then
            assertThat(actual).isEqualTo(elasticSliceResponse)
        }

        @Test
        fun `should get all orders - select api`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery {
                orderApiService.getOrdersAll(blockchains, continuation, size, sort, status)
            } returns apiSliceResponse

            // when
            val actual = service.getOrdersAll(blockchains, continuation, size, sort, status)

            // then
            assertThat(actual).isEqualTo(apiSliceResponse)
        }
    }

    @Nested
    inner class GetSellOrdersByItemTest {

        @Test
        fun `should get sell orders by item - select elastic`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery {
                orderElasticService.getSellOrdersByItem(blockchain, itemId, platform, maker, origin, status, continuation, size)
            } returns elasticSliceResponse

            // when
            val actual = service.getSellOrdersByItem(blockchain, itemId, platform, maker, origin, status, continuation, size)

            // then
            assertThat(actual).isEqualTo(elasticSliceResponse)
        }

        @Test
        fun `should get sell orders by item - select api`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery {
                orderApiService.getSellOrdersByItem(blockchain, itemId, platform, maker, origin, status, continuation, size)
            } returns apiSliceResponse

            // when
            val actual = service.getSellOrdersByItem(blockchain, itemId, platform, maker, origin, status, continuation, size)

            // then
            assertThat(actual).isEqualTo(apiSliceResponse)
        }
    }

    @Nested
    inner class GetOrderBidsByItemTest {

        @Test
        fun `should get order bids by item - select elastic`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery {
                orderElasticService.getOrderBidsByItem(blockchain, itemId, platform, makers, origin, status, start, end, continuation, size)
            } returns elasticSliceResponse

            // when
            val actual = service.getOrderBidsByItem(blockchain, itemId, platform, makers, origin, status, start, end, continuation, size)

            // then
            assertThat(actual).isEqualTo(elasticSliceResponse)
        }

        @Test
        fun `should get order bids by item - select api`() = runBlocking<Unit> {
            // given
            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery {
                orderApiService.getOrderBidsByItem(blockchain, itemId, platform, makers, origin, status, start, end, continuation, size)
            } returns apiSliceResponse

            // when
            val actual = service.getOrderBidsByItem(blockchain, itemId, platform, makers, origin, status, start, end, continuation, size)

            // then
            assertThat(actual).isEqualTo(apiSliceResponse)
        }
    }
}