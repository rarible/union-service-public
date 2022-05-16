package com.rarible.protocol.union.worker.task.search.activity

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.worker.task.search.ParamFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

internal class ChangeEsActivityAliasTaskUnitTest {

    private val completedTask = mockk<Task> {
        every { lastStatus } returns TaskStatus.COMPLETED
    }

    private val failedTask = mockk<Task> {
        every { lastStatus } returns TaskStatus.ERROR
    }

    private val reindexEthereumList = ActivityTaskParam(BlockchainDto.ETHEREUM, ActivityTypeDto.LIST, "test_activity")
    private val reindexFlowBid = ActivityTaskParam(BlockchainDto.FLOW, ActivityTypeDto.BID, "test_activity")
    private val switchAlias = ChangeEsActivityAliasTaskParam(
        "test_activity",
        listOf(reindexEthereumList, reindexFlowBid)
    )


    private val esActivityRepository = mockk<EsActivityRepository> {
        every {
            entityDefinition
        } returns mockk()

        coEvery {
            refresh()
        } returns Unit
    }

    private val indexService = mockk<IndexService> {
        coEvery {
            finishIndexing(any(), any())
        } returns Unit
    }

    private val paramFactory = ParamFactory(jacksonObjectMapper())


    @Test
    fun `should be able to run`() = runBlocking<Unit> {
        val taskRepository = mockk<TaskRepository> {
            every {
                findByTypeAndParam("ACTIVITY_REINDEX", any())
            } returns Mono.just(completedTask)
        }

        Assertions.assertThat(
            ChangeEsActivityAliasTask(
                taskRepository, esActivityRepository, indexService, paramFactory
            ).isAbleToRun(paramFactory.toString(switchAlias))
        ).isTrue()
    }

    @Test
    fun `should not be able to run`() = runBlocking<Unit> {
        val taskRepository = mockk<TaskRepository> {
            every {
                findByTypeAndParam("ACTIVITY_REINDEX", any())
            } returns Mono.just(failedTask)
        }

        Assertions.assertThat(
            ChangeEsActivityAliasTask(
                taskRepository, esActivityRepository, indexService, paramFactory
            ).isAbleToRun(paramFactory.toString(switchAlias))
        ).isFalse()
    }


}