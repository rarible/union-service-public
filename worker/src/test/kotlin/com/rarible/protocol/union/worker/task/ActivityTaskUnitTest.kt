package com.rarible.protocol.union.worker.task

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.worker.config.SearchReindexerConfiguration
import com.rarible.protocol.union.worker.config.SearchReindexerProperties
import com.rarible.protocol.union.worker.task.search.ParamFactory
import com.rarible.protocol.union.worker.task.search.activity.ActivityReindexService
import com.rarible.protocol.union.worker.task.search.activity.ActivityTask
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ActivityTaskUnitTest {

    val service = mockk<ActivityReindexService> {
        coEvery {
            reindex(any(), any(), "activity_test_index", any())
        } returns flowOf("next_cursor")
    }

    val repository = mockk<EsActivityRepository> {
        every {
            entityDefinition
        } returns EntityDefinitionExtended(
            name = EsActivity.ENTITY_DEFINITION.name,
            mapping = EsActivity.ENTITY_DEFINITION.mapping,
            versionData = EsActivity.ENTITY_DEFINITION.versionData,
            indexRootName = "activity_test_index",
            aliasName = "activity",
            writeAliasName = "activity",
            settings = EsActivity.ENTITY_DEFINITION.settings,
            reindexTaskName = EsActivity.ENTITY_DEFINITION.reindexTaskName
        )

        coEvery { refresh() } returns Unit
    }

    val indexService = mockk<IndexService> {
        coEvery {
            finishIndexing(any(), any())
        } returns Unit
    }

    @Test
    fun `should launch first run of the task`(): Unit {
        runBlocking {
            val task = ActivityTask(
                SearchReindexerConfiguration(SearchReindexerProperties()),
                ParamFactory(jacksonObjectMapper().registerKotlinModule()),
                service,
                repository,
                indexService
            )

            task.runLongTask(
                null,
                """{"blockchain": "ETHEREUM", "activityType": "LIST", "index":"activity_test_index"}"""
            ).toList()

            coVerify {
                service.reindex(BlockchainDto.ETHEREUM, ActivityTypeDto.LIST, "activity_test_index", null)
            }
        }
    }

    @Test
    fun `should launch next run of the task`(): Unit = runBlocking {
        val task = ActivityTask(
            SearchReindexerConfiguration(SearchReindexerProperties()),
            ParamFactory(jacksonObjectMapper().registerKotlinModule()),
            service,
            repository,
            indexService
        )

        task.runLongTask(
            "ETHEREUM:cursor_1",
            """{"blockchain": "ETHEREUM", "activityType": "LIST", "index":"activity_test_index"}"""
        ).toList()

        coVerify {
            service.reindex(BlockchainDto.ETHEREUM, ActivityTypeDto.LIST, "activity_test_index", "ETHEREUM:cursor_1")
        }
    }
}