package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.api.ActivityApiService
import com.rarible.protocol.union.api.service.elastic.ActivityElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
class ActivitySourceSelectServiceTest {

    @MockK
    private lateinit var featureFlagsProperties: FeatureFlagsProperties

    @MockK
    private lateinit var activityApiMergeService: ActivityApiService

    @MockK
    private lateinit var activityElasticService: ActivityElasticService

    @InjectMockKs
    private lateinit var service: ActivitySourceSelectService

    private val type = listOf(mockk<ActivityTypeDto>())
    private val blockchains = listOf(mockk<BlockchainDto>())
    private val continuation = "some continuation"
    private val cursor = "some cursor"
    private val size = 42
    private val sort = mockk<ActivitySortDto>()
    private val collection = "some collection"
    private val itemId = "some item id"
    private val userType = listOf(mockk<UserActivityTypeDto>())
    private val user = listOf("some user")
    private val from = Instant.ofEpochSecond(12345)
    private val to = Instant.ofEpochSecond(23456)

    private val apiMergeResponse = mockk<ActivitiesDto>()
    private val elasticResponse = mockk<ActivitiesDto>()

    @Nested
    inner class GetAllActivitiesTest {

        @Test
        fun `should get all activities - select elastic`() = runBlocking {
            // given
            every { featureFlagsProperties.enableActivityQueriesToElasticSearch } returns true
            coEvery {
                activityElasticService.getAllActivities(type, blockchains, continuation, cursor, size, sort)
            } returns elasticResponse

            // when
            val actual = service.getAllActivities(type, blockchains, continuation, cursor, size, sort)

            // then
            assertThat(actual).isEqualTo(elasticResponse)
            coVerify {
                activityElasticService.getAllActivities(type, blockchains, continuation, cursor, size, sort)
            }
            confirmVerified(activityApiMergeService, activityElasticService)
        }

        @Test
        fun `should get all activities - select apiMerge`() = runBlocking {
            // given
            every { featureFlagsProperties.enableActivityQueriesToElasticSearch } returns false
            coEvery {
                activityApiMergeService.getAllActivities(type, blockchains, continuation, cursor, size, sort)
            } returns apiMergeResponse

            // when
            val actual = service.getAllActivities(type, blockchains, continuation, cursor, size, sort)

            // then
            assertThat(actual).isEqualTo(apiMergeResponse)
            coVerify {
                activityApiMergeService.getAllActivities(type, blockchains, continuation, cursor, size, sort)
            }
            confirmVerified(activityApiMergeService, activityElasticService)
        }
    }

    @Nested
    inner class GetActivitiesByCollectionTest {

        @Test
        fun `should get activities by collection - select elastic`() = runBlocking {
            // given
            every { featureFlagsProperties.enableActivityQueriesToElasticSearch } returns true
            coEvery {
                activityElasticService.getActivitiesByCollection(type, collection, continuation, cursor, size, sort)
            } returns elasticResponse

            // when
            val actual = service.getActivitiesByCollection(type, collection, continuation, cursor, size, sort)

            // then
            assertThat(actual).isEqualTo(elasticResponse)
            coVerify {
                activityElasticService.getActivitiesByCollection(type, collection, continuation, cursor, size, sort)
            }
            confirmVerified(activityApiMergeService, activityElasticService)
        }

        @Test
        fun `should get activities by collection - select apiMerge`() = runBlocking {
            // given
            every { featureFlagsProperties.enableActivityQueriesToElasticSearch } returns false
            coEvery {
                activityApiMergeService.getActivitiesByCollection(type, collection, continuation, cursor, size, sort)
            } returns apiMergeResponse

            // when
            val actual = service.getActivitiesByCollection(type, collection, continuation, cursor, size, sort)

            // then
            assertThat(actual).isEqualTo(apiMergeResponse)
            coVerify {
                activityApiMergeService.getActivitiesByCollection(type, collection, continuation, cursor, size, sort)
            }
            confirmVerified(activityApiMergeService, activityElasticService)
        }
    }

    @Nested
    inner class GetActivitiesByItemTest {

        @Test
        fun `should get activities by item - select elastic`() = runBlocking {
            // given
            every { featureFlagsProperties.enableActivityQueriesToElasticSearch } returns true
            coEvery {
                activityElasticService.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)
            } returns elasticResponse

            // when
            val actual = service.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)

            // then
            assertThat(actual).isEqualTo(elasticResponse)
            coVerify {
                activityElasticService.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)
            }
            confirmVerified(activityApiMergeService, activityElasticService)
        }

        @Test
        fun `should get activities by item - select apiMerge`() = runBlocking {
            // given
            every { featureFlagsProperties.enableActivityQueriesToElasticSearch } returns false
            coEvery {
                activityApiMergeService.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)
            } returns apiMergeResponse

            // when
            val actual = service.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)

            // then
            assertThat(actual).isEqualTo(apiMergeResponse)
            coVerify {
                activityApiMergeService.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)
            }
            confirmVerified(activityApiMergeService, activityElasticService)
        }
    }

    @Nested
    inner class GetActivitiesByUserTest {

        @Test
        fun `should get activities by user - select elastic`() = runBlocking {
            // given
            every { featureFlagsProperties.enableActivityQueriesToElasticSearch } returns true
            coEvery {
                activityElasticService.getActivitiesByUser(userType, user, blockchains, from, to, continuation, cursor, size, sort)
            } returns elasticResponse

            // when
            val actual = service.getActivitiesByUser(userType, user, blockchains, from, to, continuation, cursor, size, sort)

            // then
            assertThat(actual).isEqualTo(elasticResponse)
            coVerify {
                activityElasticService.getActivitiesByUser(userType, user, blockchains, from, to, continuation, cursor, size, sort)
            }
            confirmVerified(activityApiMergeService, activityElasticService)
        }

        @Test
        fun `should get activities by user - select apiMerge`() = runBlocking {
            // given
            every { featureFlagsProperties.enableActivityQueriesToElasticSearch } returns false
            coEvery {
                activityApiMergeService.getActivitiesByUser(userType, user, blockchains, from, to, continuation, cursor, size, sort)
            } returns apiMergeResponse

            // when
            val actual = service.getActivitiesByUser(userType, user, blockchains, from, to, continuation, cursor, size, sort)

            // then
            assertThat(actual).isEqualTo(apiMergeResponse)
            coVerify {
                activityApiMergeService.getActivitiesByUser(userType, user, blockchains, from, to, continuation, cursor, size, sort)
            }
            confirmVerified(activityApiMergeService, activityElasticService)
        }
    }
}
