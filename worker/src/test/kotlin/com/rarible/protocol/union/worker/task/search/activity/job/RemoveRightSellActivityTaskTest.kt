package com.rarible.protocol.union.worker.task.search.activity.job

import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderActivityMatch
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

internal class RemoveRightSellActivityTaskTest {

    private val continuation = "345"
    private val activityId = "123"
    private val orderActivityMatch = randomEthOrderActivityMatch().copy(id = activityId)

    private val activityOrderControllerApi = mockk<OrderActivityControllerApi>() {
        coEvery {
            getOrderSellRightActivities(isNull(), any())
        } returns Mono.just(
            OrderActivitiesDto(continuation, listOf(orderActivityMatch))
        )

        coEvery {
            getOrderSellRightActivities(eq(continuation), any())
        } returns Mono.just(
            OrderActivitiesDto(
                null, emptyList()
            )
        )
    }

    private val esActivityRepository = mockk<EsActivityRepository>() {
        coEvery { deleteAll(any()) } returns 1
    }

    @Test
    fun `should launch first run of the task`(): Unit {
        runBlocking {
            val task = RemoveRightSellActivityTask(activityOrderControllerApi, esActivityRepository)

            task.runLongTask(
                null,
                ""
            ).toList()

            coVerify {
                activityOrderControllerApi.getOrderSellRightActivities(isNull(), any())
                activityOrderControllerApi.getOrderSellRightActivities(eq(continuation), any())
                esActivityRepository.deleteAll(eq(listOf(activityId)))
            }
        }
    }

    @Test
    fun `should launch next run of the task`(): Unit = runBlocking {
        val task = RemoveRightSellActivityTask(activityOrderControllerApi, esActivityRepository)

        task.runLongTask(
            continuation,
            ""
        ).toList()

        coVerify {
            activityOrderControllerApi.getOrderSellRightActivities(eq(continuation), any())
        }
    }
}