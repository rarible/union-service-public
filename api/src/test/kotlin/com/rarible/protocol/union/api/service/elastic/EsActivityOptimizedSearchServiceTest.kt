package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.api.configuration.EsOptimizationProperties

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.elastic.ElasticActivityFilter
import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivityQueryResult
import com.rarible.protocol.union.core.model.elastic.EsActivitySort
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
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
import java.time.Clock
import java.time.Duration

@ExtendWith(MockKExtension::class)
internal class EsActivityOptimizedSearchServiceTest {

    @MockK
    private lateinit var esActivityRepository: EsActivityRepository

    @SpyK
    private var properties = EsOptimizationProperties()

    @MockK
    private lateinit var clock: Clock

    @SpyK
    private var featureFlagsProperties = FeatureFlagsProperties()

    @InjectMockKs
    private lateinit var service: EsActivityOptimizedSearchService

    @Test
    fun `search - desc`() = runBlocking<Unit> {
        val filter = ElasticActivityFilter()
        val sort = EsActivitySort(latestFirst = true)
        val limit = 1
        val now = nowMillis()

        every { clock.instant() } returns now

        val last1 = now - Duration.ofHours(1)
        val expectedResult1 =
            EsActivityQueryResult(cursor = "${last1.toEpochMilli()}_1_0_0", activities = listOf(randomEsActivity()))

        coEvery {
            esActivityRepository.search(any(), any(), any())
        } returns expectedResult1

        val result1 = service.search(filter, sort, limit)
        assertThat(result1).isEqualTo(
            Slice(
                continuation = expectedResult1.cursor,
                entities = expectedResult1.activities
            )
        )

        coVerify {
            esActivityRepository.search(
                withArg {
                    assertThat(it.from).isNull()
                    assertThat(it.to).isNull()
                },
                any(),
                any()
            )
        }
    }

    @Test
    fun `search - acs`() = runBlocking<Unit> {
        val filter = ElasticActivityFilter()
        val sort = EsActivitySort(latestFirst = false)
        val limit = 1
        val now = nowMillis()

        every { clock.instant() } returns now

        val last1 = now - Duration.ofDays(10)
        val expectedResult1 =
            EsActivityQueryResult(cursor = "${last1.toEpochMilli()}_1_0_0", activities = listOf(randomEsActivity()))

        val last2 = last1 + Duration.ofHours(1)
        val expectedResult2 =
            EsActivityQueryResult(cursor = "${last2.toEpochMilli()}_1_0_0", activities = listOf(randomEsActivity()))

        coEvery {
            esActivityRepository.search(any(), any(), any())
        } returns expectedResult1

        val result1 = service.search(filter, sort, limit)
        assertThat(result1).isEqualTo(
            Slice(
                continuation = expectedResult1.cursor,
                entities = expectedResult1.activities
            )
        )
        coVerify {
            esActivityRepository.search(
                withArg {
                    val properties = EsOptimizationProperties()
                    assertThat(it.cursor).isNull()
                    assertThat(it.from).isEqualTo(properties.earliestActivityByDate)
                    assertThat(it.to).isEqualTo(
                        properties.earliestActivityByDate + properties.activityDateSearchPeriod
                    )
                },
                any(),
                any()
            )
        }

        coEvery {
            esActivityRepository.search(any(), any(), any())
        } returns expectedResult2

        val result2 = service.search(filter.copy(cursor = expectedResult1.cursor), sort, limit)
        assertThat(result2).isEqualTo(
            Slice(
                continuation = expectedResult2.cursor,
                entities = expectedResult2.activities
            )
        )
        coVerify {
            esActivityRepository.search(
                withArg {
                    assertThat(it.from).isEqualTo(last1)
                    assertThat(it.to).isEqualTo(last1 + EsOptimizationProperties().activityDateSearchPeriod)
                },
                any(),
                any()
            )
        }

        coEvery {
            esActivityRepository.search(any(), any(), any())
        } returns EsActivityQueryResult(activities = emptyList(), cursor = null)

        val result3 = service.search(filter.copy(cursor = expectedResult2.cursor), sort, limit)
        assertThat(result3).isEqualTo(Slice.empty<EsActivity>())
        coVerify {
            esActivityRepository.search(
                withArg {
                    assertThat(it.cursor).isEqualTo(expectedResult2.cursor)
                    assertThat(it.from).isEqualTo(last2)
                    assertThat(it.to).isEqualTo(last2 + EsOptimizationProperties().activityDateSearchPeriod)
                },
                any(),
                any()
            )
            esActivityRepository.search(
                withArg {
                    assertThat(it.cursor).isEqualTo(expectedResult2.cursor)
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
    fun `search - no optimizations if filters by ids`(filter: ElasticActivityFilter) = runBlocking<Unit> {
        val sort = EsActivitySort(latestFirst = false)
        val limit = 1

        val expectedResult = EsActivityQueryResult(cursor = null, activities = listOf(randomEsActivity()))

        coEvery {
            esActivityRepository.search(any(), any(), any())
        } returns expectedResult

        val result1 = service.search(filter, sort, limit)
        assertThat(result1).isEqualTo(
            Slice(
                continuation = expectedResult.cursor,
                entities = expectedResult.activities
            )
        )
        coVerify {
            esActivityRepository.search(
                withArg {
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
            ElasticActivityFilter(
                anyUsers = setOf("user")
            ),
            ElasticActivityFilter(
                usersFrom = setOf("user")
            ),
            ElasticActivityFilter(
                usersTo = setOf("user")
            ),
            ElasticActivityFilter(
                collections = setOf(randomEthCollectionId())
            ),
            ElasticActivityFilter(
                bidCurrencies = setOf(
                    CurrencyIdDto(
                        blockchain = BlockchainDto.TEZOS,
                        contract = "test",
                        tokenId = null
                    )
                )
            ),
            ElasticActivityFilter(
                items = setOf(ItemIdDto(BlockchainDto.TEZOS, "item"))
            ),
        )
    }
}
