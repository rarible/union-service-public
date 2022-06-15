package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.elastic.ActivityElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.enrichment.service.query.activity.ActivityApiMergeService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class ActivitySourceSelectServiceTest {

    @MockK
    private lateinit var featureFlagsProperties: FeatureFlagsProperties

    @MockK
    private lateinit var activityApiMergeService: ActivityApiMergeService

    @MockK
    private lateinit var activityElasticService: ActivityElasticService

    @InjectMockKs
    private lateinit var service: ActivitySourceSelectService

    private val type = listOf(mockk<ActivityTypeDto>())
    private val blockchains = listOf(mockk<BlockchainDto>())
    private val continuation = "some continuation"
    private val cursor = "some cursor"
    private val size = 42
    private val collection = "some collection"
    private val itemId = "some item id"
    private val userType = listOf(mockk<UserActivityTypeDto>())
    private val user = listOf("some user")
    private val from = Instant.ofEpochSecond(12345)
    private val to = Instant.ofEpochSecond(23456)


    companion object {
        private val apiMergeResponse = mockk<ActivitiesDto>()
        private val elasticResponse = mockk<ActivitiesDto>()

        @JvmStatic
        fun testArguments() = Stream.of(
            Arguments.of(
                true,
                null,
                false,
                ActivitySortDto.EARLIEST_FIRST,
                elasticResponse,
            ),
            Arguments.of(
                false,
                null,
                false,
                ActivitySortDto.EARLIEST_FIRST,
                apiMergeResponse,
            ),
            Arguments.of(
                true,
                OverrideSelect.API_MERGE,
                false,
                ActivitySortDto.EARLIEST_FIRST,
                apiMergeResponse,
            ),
            Arguments.of(
                false,
                OverrideSelect.ELASTIC,
                false,
                ActivitySortDto.EARLIEST_FIRST,
                elasticResponse,
            ),
            Arguments.of(
                true,
                null,
                true,
                ActivitySortDto.EARLIEST_FIRST,
                apiMergeResponse,
            ),
            Arguments.of(
                true,
                null,
                true,
                ActivitySortDto.LATEST_FIRST,
                elasticResponse,
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    fun `should get all activities`(
        elasticFeatureFlag: Boolean,
        overrideSelect: OverrideSelect?,
        ascQueriesFeatureFlag: Boolean,
        sort: ActivitySortDto,
        expectedResponse: ActivitiesDto,
    ) = runBlocking<Unit> {
        // given
        every { featureFlagsProperties.enableActivityQueriesToElasticSearch } returns elasticFeatureFlag
        every { featureFlagsProperties.enableActivityAscQueriesWithApiMerge } returns ascQueriesFeatureFlag
        coEvery {
            activityElasticService.getAllActivities(type, blockchains, continuation, cursor, size, sort)
        } returns elasticResponse
        coEvery {
            activityApiMergeService.getAllActivities(type, blockchains, continuation, cursor, size, sort)
        } returns apiMergeResponse

        // when
        val actual = service.getAllActivities(type, blockchains, continuation, cursor, size, sort, overrideSelect)

        // then
        assertThat(actual).isEqualTo(expectedResponse)
        if (expectedResponse == elasticResponse) {
            coVerify {
                activityElasticService.getAllActivities(type, blockchains, continuation, cursor, size, sort)
            }
        } else {
            coVerify {
                activityApiMergeService.getAllActivities(type, blockchains, continuation, cursor, size, sort)
            }
        }
        confirmVerified(activityApiMergeService, activityElasticService)
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    fun `should get activities by collection - select elastic`(
        elasticFeatureFlag: Boolean,
        overrideSelect: OverrideSelect?,
        ascQueriesFeatureFlag: Boolean,
        sort: ActivitySortDto,
        expectedResponse: ActivitiesDto,
    ) = runBlocking<Unit> {
        // given
        every { featureFlagsProperties.enableActivityQueriesToElasticSearch } returns elasticFeatureFlag
        every { featureFlagsProperties.enableActivityAscQueriesWithApiMerge } returns ascQueriesFeatureFlag
        coEvery {
            activityElasticService.getActivitiesByCollection(type, listOf(collection), continuation, cursor, size, sort)
        } returns elasticResponse
        coEvery {
            activityApiMergeService.getActivitiesByCollection(type, listOf(collection), continuation, cursor, size, sort)
        } returns apiMergeResponse

        // when
        val actual = service.getActivitiesByCollection(type, listOf(collection), continuation, cursor, size, sort, overrideSelect)

        // then
        assertThat(actual).isEqualTo(expectedResponse)
        if (expectedResponse == elasticResponse) {
            coVerify {
                activityElasticService.getActivitiesByCollection(type, listOf(collection), continuation, cursor, size, sort)
            }
        } else {
            coVerify {
                activityApiMergeService.getActivitiesByCollection(type, listOf(collection), continuation, cursor, size, sort)
            }
        }
        confirmVerified(activityApiMergeService, activityElasticService)
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    fun `should get activities by item - select elastic`(
        elasticFeatureFlag: Boolean,
        overrideSelect: OverrideSelect?,
        ascQueriesFeatureFlag: Boolean,
        sort: ActivitySortDto,
        expectedResponse: ActivitiesDto,
    ) = runBlocking<Unit> {
        // given
        every { featureFlagsProperties.enableActivityQueriesToElasticSearch } returns elasticFeatureFlag
        every { featureFlagsProperties.enableActivityAscQueriesWithApiMerge } returns ascQueriesFeatureFlag
        coEvery {
            activityElasticService.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)
        } returns elasticResponse
        coEvery {
            activityApiMergeService.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)
        } returns apiMergeResponse

        // when
        val actual = service.getActivitiesByItem(type, itemId, continuation, cursor, size, sort, overrideSelect)

        // then
        assertThat(actual).isEqualTo(expectedResponse)
        if (expectedResponse == elasticResponse) {
            coVerify {
                activityElasticService.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)
            }
        } else {
            coVerify {
                activityApiMergeService.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)
            }
        }
        confirmVerified(activityApiMergeService, activityElasticService)
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    fun `should get activities by user - select elastic`(
        elasticFeatureFlag: Boolean,
        overrideSelect: OverrideSelect?,
        ascQueriesFeatureFlag: Boolean,
        sort: ActivitySortDto,
        expectedResponse: ActivitiesDto,
    ) = runBlocking<Unit> {
        // given
        every { featureFlagsProperties.enableActivityQueriesToElasticSearch } returns elasticFeatureFlag
        every { featureFlagsProperties.enableActivityAscQueriesWithApiMerge } returns ascQueriesFeatureFlag
        coEvery {
            activityElasticService.getActivitiesByUser(userType, user, blockchains, from, to, continuation, cursor, size, sort)
        } returns elasticResponse
        coEvery {
            activityApiMergeService.getActivitiesByUser(userType, user, blockchains, from, to, continuation, cursor, size, sort)
        } returns apiMergeResponse

        // when
        val actual = service.getActivitiesByUser(userType, user, blockchains, from, to, continuation, cursor, size, sort, overrideSelect)

        // then
        assertThat(actual).isEqualTo(expectedResponse)
        if (expectedResponse == elasticResponse) {
            coVerify {
                activityElasticService.getActivitiesByUser(userType, user, blockchains, from, to, continuation, cursor, size, sort)
            }
        } else {
            coVerify {
                activityApiMergeService.getActivitiesByUser(userType, user, blockchains, from, to, continuation, cursor, size, sort)
            }
        }
        confirmVerified(activityApiMergeService, activityElasticService)
    }
}
