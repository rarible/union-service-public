package com.rarible.protocol.union.worker.task

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.worker.config.SearchReindexerConfiguration
import com.rarible.protocol.union.worker.config.SearchReindexerProperties
import com.rarible.protocol.union.worker.task.search.ParamFactory
import com.rarible.protocol.union.worker.task.search.activity.ActivityTask
import com.rarible.protocol.union.worker.task.search.activity.ActivityTaskParam
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.time.Instant
import kotlin.random.Random

class ActivityTaskUnitTest {

    val esActivityRepository = mockk<EsActivityRepository> {
        coEvery {
            saveAll(any(), "activity_test_index")
        } answers { arg(0) }
    }

    val converter = mockk<EsActivityConverter> {
        every {
            convert(any<OrderListActivityDto>())
        } returns EsActivity(
            randomActivityId().fullId(),
            Instant.now(),
            0xb1,
            42,
            Random(0).nextLong(),
            BlockchainDto.ETHEREUM,
            ActivityTypeDto.LIST,
            "0x01",
            null,
            "0xc0",
            "0xa0",
        )
    }

    val activityClient = mockk<ActivityControllerApi> {
        every {
            getAllActivities(
                listOf(ActivityTypeDto.LIST),
                listOf(BlockchainDto.ETHEREUM),
                null,
                null,
                ActivityTask.PAGE_SIZE,
                ActivitySortDto.EARLIEST_FIRST
            )
        } returns Mono.just(
            ActivitiesDto(
                "ETHEREUM:cursor_1", "ETHEREUM:cursor_1", listOf(
                    mockk()
                )
            )
        )

        every {
            getAllActivities(
                listOf(ActivityTypeDto.LIST),
                listOf(BlockchainDto.ETHEREUM),
                "ETHEREUM:cursor_1",
                "ETHEREUM:cursor_1",
                ActivityTask.PAGE_SIZE,
                ActivitySortDto.EARLIEST_FIRST
            )
        } returns Mono.just(
            ActivitiesDto(
                null, null, listOf(
                    mockk()
                )
            )
        )
    }

    @Test
    fun `should launch first run of the task`(): Unit {

        runBlocking {
            val task = ActivityTask(
                SearchReindexerConfiguration(SearchReindexerProperties()),
                activityClient,
                esActivityRepository,
                converter,
                ParamFactory(jacksonObjectMapper().registerKotlinModule())
            )

            task.runLongTask(
                null,
                """{"blockchain": "ETHEREUM", "activityType": "LIST", "index":"activity_test_index"}"""
            ).toList()

            coVerify {
                activityClient.getAllActivities(
                    listOf(ActivityTypeDto.LIST),
                    listOf(BlockchainDto.ETHEREUM),
                    null,
                    null,
                    ActivityTask.PAGE_SIZE,
                    ActivitySortDto.EARLIEST_FIRST
                )

                converter.convert(any<OrderListActivityDto>())

                esActivityRepository.saveAll(any(), "activity_test_index")
            }
        }
    }

    @Test
    fun `should launch next run of the task`(): Unit = runBlocking {
        val task = ActivityTask(
            SearchReindexerConfiguration(SearchReindexerProperties()),
            activityClient,
            esActivityRepository,
            converter,
            ParamFactory(jacksonObjectMapper().registerKotlinModule())
        )

        task.runLongTask(
            "ETHEREUM:cursor_1",
            """{"blockchain": "ETHEREUM", "activityType": "LIST", "index":"activity_test_index"}"""
        ).toList()

        coVerify {
            activityClient.getAllActivities(
                listOf(ActivityTypeDto.LIST),
                listOf(BlockchainDto.ETHEREUM),
                "ETHEREUM:cursor_1",
                "ETHEREUM:cursor_1",
                ActivityTask.PAGE_SIZE,
                ActivitySortDto.EARLIEST_FIRST
            )

            converter.convert(any<OrderListActivityDto>())

            esActivityRepository.saveAll(any(), "activity_test_index")
        }
    }

    companion object {
        private fun randomActivityId(): ActivityIdDto {
            return ActivityIdDto(
                blockchain = BlockchainDto.ETHEREUM,
                value = randomAddress().toString()
            )
        }
    }
}