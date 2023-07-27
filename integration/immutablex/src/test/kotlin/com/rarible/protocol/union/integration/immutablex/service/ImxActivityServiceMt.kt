package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.test.ManualTest
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.integration.ImxManualTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

@ManualTest
class ImxActivityServiceMt : ImxManualTest() {

    private val service = ImxActivityService(activityClient, orderClient, imxActivityConverter)

    @Test
    fun getByIds() = runBlocking<Unit> {
        val ids = listOf(
            TypedActivityId("5132646", ActivityTypeDto.TRANSFER),
            TypedActivityId("5133012", ActivityTypeDto.MINT),
            TypedActivityId("5132321", ActivityTypeDto.SELL),
            TypedActivityId("5112646", ActivityTypeDto.CANCEL_LIST) // Should be not found
        )

        val result = service.getActivitiesByIds(ids)
        val mapped = result.associateBy { it.id.value }

        println(result)
        assertThat(result).hasSize(3)
        assertThat(mapped["5132646"]).isInstanceOf(TransferActivityDto::class.java)
        assertThat(mapped["5133012"]).isInstanceOf(MintActivityDto::class.java)
        assertThat(mapped["5132321"]).isInstanceOf(OrderMatchSellDto::class.java)
    }

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
            owner = "0xf42eca6ccfaab740a962317ce6506639f3588888", // Non-existing user
            continuation = null,
            size = 10,
            sort = ActivitySortDto.LATEST_FIRST
        ).entities

        println(activitiesWithWrongUser)
        assertThat(activitiesWithWrongUser).hasSize(0)
    }

    @Test
    fun `get transfers - no burns`() = runBlocking<Unit> {
        // This user burned 4 items (the latest transactions), they should be filtered out
        val page1 = service.getActivitiesByUser(
            types = listOf(UserActivityTypeDto.TRANSFER_FROM),
            users = listOf("0x9041c8804999a4a741f10011ba284b7925c45740"),
            from = null,
            to = null,
            continuation = null,
            size = 5,
            sort = ActivitySortDto.LATEST_FIRST
        )

        val page2 = service.getActivitiesByUser(
            types = listOf(UserActivityTypeDto.TRANSFER_FROM),
            users = listOf("0x9041c8804999a4a741f10011ba284b7925c45740"),
            from = null,
            to = null,
            continuation = page1.continuation,
            size = 5,
            sort = ActivitySortDto.LATEST_FIRST
        )

        // There are 7 transfers in total (and 4 filtered burns)
        assertThat(page1.entities).hasSize(5)
        assertThat(page2.entities).hasSize(2)
    }

    @Test
    fun `get transfers - burns & transfers`() = runBlocking<Unit> {
        // We expect here single request returns burns and transfers together
        // (this user has 4 burns and 7 transfers in total)
        val page = service.getActivitiesByUser(
            types = listOf(UserActivityTypeDto.TRANSFER_FROM, UserActivityTypeDto.BURN),
            users = listOf("0x9041c8804999a4a741f10011ba284b7925c45740"),
            from = null,
            to = null,
            continuation = null,
            size = 10,
            sort = ActivitySortDto.LATEST_FIRST
        )

        assertThat(page.entities).hasSize(10)
    }

    @Test
    fun `get transfers - only burns`() = runBlocking<Unit> {
        val page = service.getActivitiesByUser(
            types = listOf(UserActivityTypeDto.BURN),
            users = listOf("0x9041c8804999a4a741f10011ba284b7925c45740"),
            from = null,
            to = null,
            continuation = null,
            size = 5,
            sort = ActivitySortDto.LATEST_FIRST
        )

        assertThat(page.entities).hasSize(4)
    }

    @Test
    fun `same timestamp on page break`() = runBlocking<Unit> {
        val continuation = "1634727056071_2240398"
        val page1 = service.getAllActivities(
            types = listOf(ActivityTypeDto.MINT),
            continuation = continuation,
            size = 2,
            sort = ActivitySortDto.EARLIEST_FIRST
        )
        val page2 = service.getAllActivities(
            types = listOf(ActivityTypeDto.MINT),
            continuation = page1.continuation,
            size = 2,
            sort = ActivitySortDto.EARLIEST_FIRST
        )
        val page3 = service.getAllActivities(
            types = listOf(ActivityTypeDto.MINT),
            continuation = page2.continuation,
            size = 2,
            sort = ActivitySortDto.EARLIEST_FIRST
        )

        val result = page1.entities + page2.entities + page3.entities
        val dates = result.map { it.date }.toSet()
        // All entities have same date
        assertThat(dates).hasSize(1)

        val ids = result.map { it.id.value }
        assertThat(ids).isEqualTo(
            listOf("2240399", "2240400", "2240401", "2240402", "2240403", "2240404")
        )
    }

    @Test
    fun getLastSaleByItem() = runBlocking<Unit> {
        val trades = service.getActivitiesByItem(
            types = listOf(ActivityTypeDto.SELL),
            itemId = "0x673b1ae1652dd850aea52cf3f793ce86831d1b8c:1000",
            continuation = null,
            size = 1,
            ActivitySortDto.LATEST_FIRST
        ).entities

        assertThat(trades).hasSize(1)
    }
}
