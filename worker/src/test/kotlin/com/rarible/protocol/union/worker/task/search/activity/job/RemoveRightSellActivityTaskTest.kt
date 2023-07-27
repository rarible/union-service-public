package com.rarible.protocol.union.worker.task.search.activity.job

import com.rarible.protocol.dto.IdsDto
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
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
    private val orderActivityMatch = listOf(activityId)

    private val ethereumActivityOrderControllerApi = mockk<OrderActivityControllerApi> {
        coEvery {
            getOrderSellRightActivities(isNull(), any())
        } returns Mono.just(
            IdsDto(continuation, orderActivityMatch)
        )

        coEvery {
            getOrderSellRightActivities(eq(continuation), any())
        } returns Mono.just(
            IdsDto(
                null, emptyList()
            )
        )
    }
    private val polygonActivityOrderControllerApi = mockk<OrderActivityControllerApi>()

    private val esActivityRepository = mockk<EsActivityRepository> {
        coEvery {
            deleteAll(any())
        } returns Unit
    }

    @Test
    fun `should launch first run of the task`() {
        runBlocking {
            val task = RemoveRightSellActivityTask(
                ethereumActivityOrderControllerApi,
                polygonActivityOrderControllerApi,
                esActivityRepository
            )

            task.runLongTask(
                null,
                ""
            ).toList()

            coVerify {
                ethereumActivityOrderControllerApi.getOrderSellRightActivities(isNull(), any())
                ethereumActivityOrderControllerApi.getOrderSellRightActivities(eq(continuation), any())
                esActivityRepository.deleteAll(eq(listOf(activityId)))
            }
        }
    }

    @Test
    fun `should launch next run of the task`(): Unit = runBlocking {
        val task = RemoveRightSellActivityTask(
            ethereumActivityOrderControllerApi,
            polygonActivityOrderControllerApi,
            esActivityRepository
        )

        task.runLongTask(
            continuation,
            ""
        ).toList()

        coVerify {
            ethereumActivityOrderControllerApi.getOrderSellRightActivities(eq(continuation), any())
        }
    }
}
