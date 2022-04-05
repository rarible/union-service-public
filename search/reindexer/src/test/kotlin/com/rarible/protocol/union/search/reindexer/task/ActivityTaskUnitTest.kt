package com.rarible.protocol.union.search.reindexer.task

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.search.core.ElasticActivity
import com.rarible.protocol.union.search.core.converter.ElasticActivityConverter
import com.rarible.protocol.union.search.reindexer.config.SearchReindexerConfiguration
import com.rarible.protocol.union.search.reindexer.config.SearchReindexerProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import reactor.core.publisher.Mono
import java.time.Instant

class ActivityTaskUnitTest {

    val esOperations = mockk<ElasticsearchOperations> {
        every {
            save(any<Iterable<ElasticActivity>>())
        } answers { arg(0) }
    }

    val converter = mockk<ElasticActivityConverter> {
        every {
            convert(any<OrderListActivityDto>())
        } returns ElasticActivity(
            randomActivityId().fullId(),
            Instant.now(),
            0xb1,
            42,
            BlockchainDto.ETHEREUM,
            ActivityTypeDto.LIST,
            ElasticActivity.User(
                "0x01", null
            ),
            ElasticActivity.Collection("0xc0"),
            ElasticActivity.Item("0xa0")
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
        ))

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
            ))
    }

    @Test
    fun `should launch first run of the task`(): Unit = runBlocking {
        val task = ActivityTask(
            SearchReindexerConfiguration(SearchReindexerProperties()),
            activityClient,
            mockk(),
            esOperations,
            converter
        )

        task.runLongTask(
            null,
            "ACTIVITY_ETHEREUM_LIST"
        ).toList()

        verify {
            activityClient.getAllActivities(
                listOf(ActivityTypeDto.LIST),
                listOf(BlockchainDto.ETHEREUM),
                null,
                null,
                ActivityTask.PAGE_SIZE,
                ActivitySortDto.EARLIEST_FIRST
            )

            converter.convert(any<OrderListActivityDto>())

            esOperations.save(any<Iterable<ElasticActivity>>())
        }
    }

    @Test
    fun `should launch next run of the task`(): Unit = runBlocking {
        val task = ActivityTask(
            SearchReindexerConfiguration(SearchReindexerProperties()),
            activityClient,
            mockk(),
            esOperations,
            converter
        )

        task.runLongTask(
            "ETHEREUM:cursor_1",
            "ACTIVITY_ETHEREUM_LIST"
        ).toList()

        verify {
            activityClient.getAllActivities(
                listOf(ActivityTypeDto.LIST),
                listOf(BlockchainDto.ETHEREUM),
                "ETHEREUM:cursor_1",
                "ETHEREUM:cursor_1",
                ActivityTask.PAGE_SIZE,
                ActivitySortDto.EARLIEST_FIRST
            )

            converter.convert(any<OrderListActivityDto>())

            esOperations.save(any<Iterable<ElasticActivity>>())
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