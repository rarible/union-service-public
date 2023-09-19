package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.EsOptimizationProperties
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.elastic.EsOwnership
import com.rarible.protocol.union.core.model.elastic.EsOwnershipSort
import com.rarible.protocol.union.core.model.elastic.EsOwnershipsSearchFilter
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.enrichment.repository.search.internal.EsEntitySearchAfterCursorService
import com.rarible.protocol.union.enrichment.test.data.randomUnionAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import randomAuctionId
import randomEsOwnership
import randomItemId
import java.time.Clock
import java.time.Duration

@ExtendWith(MockKExtension::class)
internal class EsOwnershipOptimizedSearchServiceTest {

    @MockK
    private lateinit var esOwnershipRepository: EsOwnershipRepository

    @SpyK
    private var properties = EsOptimizationProperties()

    @SpyK
    private var esEntitySearchAfterCursorService = EsEntitySearchAfterCursorService()

    @MockK
    private lateinit var clock: Clock

    @SpyK
    private var featureFlagsProperties = FeatureFlagsProperties()

    @InjectMockKs
    private lateinit var service: EsOwnershipOptimizedSearchService

    @Test
    fun `search - desc`() = runBlocking<Unit> {
        val filter = EsOwnershipsSearchFilter()
        val sort = EsOwnershipSort.LATEST_FIRST
        val limit = 1
        val now = nowMillis()

        every { clock.instant() } returns now

        val last1 = now - Duration.ofHours(1)
        val expectedResult1 =
            Slice(continuation = "${last1.toEpochMilli()}_1_0_0", entities = listOf(randomEsOwnership()))

        coEvery {
            esOwnershipRepository.search(any(), any(), any())
        } returns expectedResult1

        val result1 = service.search(filter, sort, limit)
        assertThat(result1).isEqualTo(expectedResult1)

        coVerify {
            esOwnershipRepository.search(
                withArg {
                    it as EsOwnershipsSearchFilter
                    assertThat(it.afterDate).isNull()
                    assertThat(it.beforeDate).isNull()
                },
                any(),
                any()
            )
        }
    }

    @Test
    fun `search - acs`() = runBlocking<Unit> {
        val filter = EsOwnershipsSearchFilter()
        val sort = EsOwnershipSort.EARLIEST_FIRST
        val limit = 1
        val now = nowMillis()

        every { clock.instant() } returns now

        val last1 = now - Duration.ofDays(10)
        val expectedResult1 =
            Slice(continuation = "${last1.toEpochMilli()}_1", entities = listOf(randomEsOwnership()))

        val last2 = last1 + Duration.ofHours(1)
        val expectedResult2 =
            Slice(continuation = "${last2.toEpochMilli()}_1", entities = listOf(randomEsOwnership()))

        coEvery {
            esOwnershipRepository.search(any(), any(), any())
        } returns expectedResult1

        val result1 = service.search(filter, sort, limit)
        assertThat(result1).isEqualTo(expectedResult1)
        coVerify {
            esOwnershipRepository.search(
                withArg {
                    it as EsOwnershipsSearchFilter
                    val properties = EsOptimizationProperties()
                    assertThat(it.cursor).isNull()
                    assertThat(it.from).isEqualTo(properties.earliestOwnershipDate)
                    assertThat(it.to).isEqualTo(
                        properties.earliestOwnershipDate + properties.ownershipDateSearchPeriod
                    )
                },
                any(),
                any()
            )
        }

        coEvery {
            esOwnershipRepository.search(any(), any(), any())
        } returns expectedResult2

        val result2 = service.search(filter.copy(cursor = expectedResult1.continuation), sort, limit)
        assertThat(result2).isEqualTo(expectedResult2)
        coVerify {
            esOwnershipRepository.search(
                withArg {
                    it as EsOwnershipsSearchFilter
                    assertThat(it.from).isEqualTo(last1)
                    assertThat(it.to).isEqualTo(last1 + EsOptimizationProperties().ownershipDateSearchPeriod)
                },
                any(),
                any()
            )
        }

        coEvery {
            esOwnershipRepository.search(any(), any(), any())
        } returns Slice(entities = emptyList(), continuation = null)

        val result3 = service.search(filter.copy(cursor = expectedResult2.continuation), sort, limit)
        assertThat(result3).isEqualTo(Slice.empty<EsOwnership>())
        coVerify {
            esOwnershipRepository.search(
                withArg {
                    it as EsOwnershipsSearchFilter
                    assertThat(it.cursor).isEqualTo(expectedResult2.continuation)
                    assertThat(it.from).isEqualTo(last2)
                    assertThat(it.to).isEqualTo(last2 + EsOptimizationProperties().ownershipDateSearchPeriod)
                },
                any(),
                any()
            )
            esOwnershipRepository.search(
                withArg {
                    it as EsOwnershipsSearchFilter
                    assertThat(it.cursor).isEqualTo(expectedResult2.continuation)
                    assertThat(it.from).isNull()
                    assertThat(it.to).isNull()
                },
                any(),
                any()
            )
        }
    }

    @ParameterizedTest
    @MethodSource("noOptimizationFilters")
    fun `search - no optimizations if filters by ids`(filter: EsOwnershipsSearchFilter) = runBlocking<Unit> {
        val sort = EsOwnershipSort.EARLIEST_FIRST
        val limit = 1

        val expectedResult = Slice(continuation = null, entities = listOf(randomEsOwnership()))

        coEvery {
            esOwnershipRepository.search(any(), any(), any())
        } returns expectedResult

        val result1 = service.search(filter, sort, limit)
        assertThat(result1).isEqualTo(expectedResult)
        coVerify {
            esOwnershipRepository.search(
                withArg {
                    it as EsOwnershipsSearchFilter
                    assertThat(it.cursor).isNull()
                    assertThat(it.from).isNull()
                    assertThat(it.to).isNull()
                },
                any(),
                any()
            )
        }
    }

    companion object {
        @JvmStatic
        fun noOptimizationFilters() = listOf(
            EsOwnershipsSearchFilter(
                collections = listOf(randomEthCollectionId())
            ),
            EsOwnershipsSearchFilter(
                owners = listOf(randomUnionAddress())
            ),
            EsOwnershipsSearchFilter(
                items = listOf(randomItemId())
            ),
            EsOwnershipsSearchFilter(
                auctions = listOf(randomAuctionId())
            ),
            EsOwnershipsSearchFilter(
                auctionOwners = listOf(randomUnionAddress())
            ),
        )
    }
}
