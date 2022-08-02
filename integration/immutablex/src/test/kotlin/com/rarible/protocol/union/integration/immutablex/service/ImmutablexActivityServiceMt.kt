package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.test.ManualTest
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.integration.ImmutablexManualTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

@ManualTest
class ImmutablexActivityServiceMt : ImmutablexManualTest() {

    private val orderService = ImmutablexOrderService(orderClient)
    private val service = ImmutablexActivityService(activityClient, orderService)

    @Test
    fun `getAllActivities - mints, desc`() = runBlocking<Unit> {
        val activities = service.getAllActivities(
            types = listOf(ActivityTypeDto.MINT),
            size = 2,
            sort = ActivitySortDto.LATEST_FIRST,
            continuation = null
        ).entities

        println(activities)
        assertThat(activities.size).isEqualTo(2)
        assertThat(activities[0].date).isAfterOrEqualTo(activities[1].date)
    }

    @Test
    fun `getAllActivities - transfers, asc`() = runBlocking<Unit> {
        val activities = service.getAllActivities(
            types = listOf(ActivityTypeDto.TRANSFER),
            size = 2,
            sort = ActivitySortDto.EARLIEST_FIRST,
            continuation = null
        ).entities

        println(activities)
        assertThat(activities.size).isEqualTo(2)
        assertThat(activities[0].date).isBeforeOrEqualTo(activities[1].date)
    }

    @Test
    fun `getAllActivities - trades, desc`() = runBlocking<Unit> {
        val activities = service.getAllActivities(
            types = listOf(ActivityTypeDto.SELL),
            size = 2,
            sort = ActivitySortDto.LATEST_FIRST,
            continuation = null
        ).entities

        println(activities)
        // Just ensure there is no exceptions related to missing orders
        assertThat(activities.size).isEqualTo(2)
        assertThat(activities[0].date).isAfterOrEqualTo(activities[1].date)
    }

    @Test
    fun `getActivitiesByUser - transfers with continuation, asc`() = runBlocking<Unit> {
        val from = Instant.parse("2022-08-01T02:20:00Z")
        val to = Instant.parse("2022-08-01T02:25:00Z")
        val continuationDate = Instant.parse("2022-08-01T02:24:00Z")

        val allActivities = service.getActivitiesByUser(
            types = listOf(UserActivityTypeDto.TRANSFER_FROM),
            from = from,
            to = to,
            users = listOf("0xef370dd9de2a74945b613afa1bf028158ed8087e"),
            size = 100,
            sort = ActivitySortDto.EARLIEST_FIRST,
            continuation = null
        ).entities

        println(allActivities)
        assertThat(allActivities).hasSize(2)

        // One of the activities here has date about 2022-08-01T02:24:00Z, trying to get it using continuation
        val activitiesWithContinuation = service.getActivitiesByUser(
            types = listOf(UserActivityTypeDto.TRANSFER_FROM),
            from = from,
            to = to,
            users = listOf("0xef370dd9de2a74945b613afa1bf028158ed8087e"),
            size = 100,
            sort = ActivitySortDto.EARLIEST_FIRST,
            continuation = "${continuationDate.toEpochMilli()}_1"
        ).entities

        println(activitiesWithContinuation)
        assertThat(activitiesWithContinuation).hasSize(1)
    }

    @Test
    fun getByItem() = runBlocking<Unit> {
        val activities = service.getActivitiesByItem(
            types = listOf(),
            itemId = "0xb2d73b6a1da13882c15ca7e248051e38f0abd1e6:2083",
            continuation = null,
            size = 10,
            sort = ActivitySortDto.LATEST_FIRST
        ).entities

        println(activities)
        assertThat(activities).hasSize(2)
    }

    @Test
    fun getByCollection() = runBlocking<Unit> {
        val activities = service.getActivitiesByCollection(
            types = listOf(),
            collection = "0xb2d73b6a1da13882c15ca7e248051e38f0abd1e6",
            continuation = null,
            size = 10,
            sort = ActivitySortDto.LATEST_FIRST
        ).entities

        println(activities)
        assertThat(activities).hasSize(10)
    }

    @Test
    fun getActivitiesByItemAndOwner() = runBlocking<Unit> {
        val activitiesWithRightUser = service.getActivitiesByItemAndOwner(
            types = listOf(),
            itemId = "0xb2d73b6a1da13882c15ca7e248051e38f0abd1e6:2083",
            owner = "0xf42eca6ccfaab740a962317ce6506639f3561690",
            continuation = null,
            size = 10,
            sort = ActivitySortDto.LATEST_FIRST
        ).entities

        println(activitiesWithRightUser)
        assertThat(activitiesWithRightUser).hasSize(2)

        val activitiesWithWrongUser = service.getActivitiesByItemAndOwner(
            types = listOf(),
            itemId = "0xb2d73b6a1da13882c15ca7e248051e38f0abd1e6:2083",
            owner = "0xf42eca6ccfaab740a962317ce6506639f3588888", //Non-existing user
            continuation = null,
            size = 10,
            sort = ActivitySortDto.LATEST_FIRST
        ).entities

        println(activitiesWithWrongUser)
        assertThat(activitiesWithWrongUser).hasSize(0)
    }

}