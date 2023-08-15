package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.api.configuration.EsOptimizationProperties
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.elastic.EsItemFilter
import com.rarible.protocol.union.core.model.elastic.EsItemGenericFilter
import com.rarible.protocol.union.core.model.elastic.EsItemLite
import com.rarible.protocol.union.core.model.elastic.EsItemSort
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.internal.EsEntitySearchAfterCursorService
import com.rarible.protocol.union.enrichment.test.data.randomEsItemLite
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration

class EsItemOptimizedSearchServiceTest {
    private val esItemRepository = mockk<EsItemRepository>()
    private val esEntitySearchAfterCursorService = EsEntitySearchAfterCursorService()
    private val properties = EsOptimizationProperties()
    private val clock = mockk<Clock>()

    private val service = EsItemOptimizedSearchService(
        esItemRepository,
        esEntitySearchAfterCursorService,
        properties,
        clock,
        FeatureFlagsProperties(enableOptimizedSearchForItems = true)
    )

    @Test
    fun `search - desc`() = runBlocking<Unit> {
        val filter = EsItemGenericFilter()
        val sort = EsItemSort.LATEST_FIRST
        val limit = 1
        val now = nowMillis()

        every { clock.instant() } returns now

        val last1 = now - Duration.ofHours(1)
        val expectedResult1 = Slice(continuation = "${last1.toEpochMilli()}_1", listOf(randomEsItemLite()))

        val last2 = last1 - Duration.ofHours(1)
        val expectedResult2 = Slice(continuation = "${last2.toEpochMilli()}_1", listOf(randomEsItemLite()))

        coEvery {
            esItemRepository.search(any<EsItemFilter>(), any(), any())
        } returns expectedResult1
        val result1 = service.search(filter, sort, limit)
        assertThat(result1).isEqualTo(expectedResult1)

        coVerify {
            esItemRepository.search(
                withArg {
                    it as EsItemGenericFilter
                    assertThat(it.updatedFrom).isEqualTo(now - properties.lastUpdatedSearchPeriod)
                    assertThat(it.updatedTo).isEqualTo(now)
                },
                any(),
                any()
            )
        }

        coEvery {
            esItemRepository.search(any<EsItemFilter>(), any(), any())
        } returns expectedResult2
        val result2 = service.search(filter.copy(cursor = expectedResult1.continuation), sort, limit)
        assertThat(result2).isEqualTo(expectedResult2)

        coVerify {
            esItemRepository.search(
                withArg {
                    it as EsItemGenericFilter
                    assertThat(it.updatedFrom).isEqualTo(last1 - properties.lastUpdatedSearchPeriod)
                    assertThat(it.updatedTo).isEqualTo(last1)
                },
                any(),
                any()
            )
        }

        coEvery {
            esItemRepository.search(any<EsItemFilter>(), any(), any())
        } returns Slice.empty()
        val result3 = service.search(filter.copy(cursor = expectedResult2.continuation), sort, limit)
        assertThat(result3).isEqualTo(Slice.empty<EsItemLite>())

        coVerify {
            esItemRepository.search(
                withArg {
                    it as EsItemGenericFilter
                    assertThat(it.cursor).isEqualTo(expectedResult2.continuation)
                    assertThat(it.updatedFrom).isEqualTo(last2 - properties.lastUpdatedSearchPeriod)
                    assertThat(it.updatedTo).isEqualTo(last2)
                },
                any(),
                any()
            )
            esItemRepository.search(
                withArg {
                    it as EsItemGenericFilter
                    assertThat(it.cursor).isEqualTo(expectedResult2.continuation)
                    assertThat(it.updatedFrom).isNull()
                    assertThat(it.updatedTo).isNull()
                },
                any(),
                any()
            )
        }
    }

    @Test
    fun `search - acs`() = runBlocking<Unit> {
        val filter = EsItemGenericFilter()
        val sort = EsItemSort.EARLIEST_FIRST
        val limit = 1
        val now = nowMillis()

        every { clock.instant() } returns now

        val last1 = now - Duration.ofHours(10)
        val expectedResult1 = Slice(continuation = "${last1.toEpochMilli()}_1", listOf(randomEsItemLite()))

        val last2 = last1 + Duration.ofHours(1)
        val expectedResult2 = Slice(continuation = "${last2.toEpochMilli()}_1", listOf(randomEsItemLite()))

        coEvery {
            esItemRepository.search(any<EsItemFilter>(), any(), any())
        } returns expectedResult1

        val result1 = service.search(filter, sort, limit)
        assertThat(result1).isEqualTo(expectedResult1)
        coVerify {
            esItemRepository.search(
                withArg {
                    it as EsItemGenericFilter
                    assertThat(it.cursor).isNull()
                    assertThat(it.updatedFrom).isEqualTo(properties.earliestItemByLastUpdateAt)
                    assertThat(it.updatedTo).isEqualTo(
                        properties.earliestItemByLastUpdateAt + properties.lastUpdatedSearchPeriod
                    )
                },
                any(),
                any()
            )
        }

        coEvery {
            esItemRepository.search(any<EsItemFilter>(), any(), any())
        } returns expectedResult2

        val result2 = service.search(filter.copy(cursor = expectedResult1.continuation), sort, limit)
        assertThat(result2).isEqualTo(expectedResult2)
        coVerify {
            esItemRepository.search(
                withArg {
                    it as EsItemGenericFilter
                    assertThat(it.updatedFrom).isEqualTo(last1)
                    assertThat(it.updatedTo).isEqualTo(last1 + properties.lastUpdatedSearchPeriod)
                },
                any(),
                any()
            )
        }

        coEvery {
            esItemRepository.search(any<EsItemFilter>(), any(), any())
        } returns Slice.empty()

        val result3 = service.search(filter.copy(cursor = expectedResult2.continuation), sort, limit)
        assertThat(result3).isEqualTo(Slice.empty<EsItemLite>())
        coVerify {
            esItemRepository.search(
                withArg {
                    it as EsItemGenericFilter
                    assertThat(it.cursor).isEqualTo(expectedResult2.continuation)
                    assertThat(it.updatedFrom).isEqualTo(last2)
                    assertThat(it.updatedTo).isEqualTo(last2 + properties.lastUpdatedSearchPeriod)
                },
                any(),
                any()
            )
            esItemRepository.search(
                withArg {
                    it as EsItemGenericFilter
                    assertThat(it.cursor).isEqualTo(expectedResult2.continuation)
                    assertThat(it.updatedFrom).isNull()
                    assertThat(it.updatedTo).isNull()
                },
                any(),
                any()
            )
        }
    }

    @Test
    fun `search - no optimizations if filters by ids`() = runBlocking<Unit> {
        val filter = EsItemGenericFilter(itemIds = setOf(randomString()))
        val sort = EsItemSort.EARLIEST_FIRST
        val limit = 1

        val expectedResult = Slice(continuation = null, listOf(randomEsItemLite()))

        coEvery {
            esItemRepository.search(any<EsItemFilter>(), any(), any())
        } returns expectedResult

        val result1 = service.search(filter, sort, limit)
        assertThat(result1).isEqualTo(expectedResult)
        coVerify {
            esItemRepository.search(
                withArg {
                    it as EsItemGenericFilter
                    assertThat(it.cursor).isNull()
                    assertThat(it.updatedFrom).isNull()
                    assertThat(it.updatedTo).isNull()
                },
                any(),
                any()
            )
        }
    }
}
