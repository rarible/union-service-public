package com.rarible.protocol.union.search.reindexer.task

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.worker.config.BlockchainReindexProperties
import com.rarible.protocol.union.worker.config.OwnershipReindexProperties
import com.rarible.protocol.union.worker.task.search.ParamFactory
import com.rarible.protocol.union.worker.task.search.ownership.OwnershipReindexService
import com.rarible.protocol.union.worker.task.search.ownership.OwnershipTask
import com.rarible.protocol.union.worker.task.search.ownership.OwnershipTaskParam
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class OwnershipTaskUnitTest {

    private val reindexService = mockk<OwnershipReindexService> {
        coEvery {
            reindex(any(), any(), "ownership_test_index", any())
        } returns flowOf("next_cursor")
    }

    private val repository = mockk<EsOwnershipRepository> {
        every {
            entityDefinition
        } returns EntityDefinitionExtended(
            entity = EsOwnership.ENTITY_DEFINITION.entity,
            mapping = EsOwnership.ENTITY_DEFINITION.mapping,
            versionData = EsOwnership.ENTITY_DEFINITION.versionData,
            indexRootName = "ownership_test_index",
            aliasName = "ownership",
            writeAliasName = "ownership",
            settings = EsOwnership.ENTITY_DEFINITION.settings
        )

        coEvery { refresh() } returns Unit
    }

    private val indexService = mockk<IndexService> {
        coEvery {
            finishIndexing(any(), any())
        } returns Unit
    }

    @Test
    fun `should launch first run of the task`() = runBlocking {
        val task = OwnershipTask(
            OwnershipReindexProperties(
                enabled = true,
                blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
            ),
            ParamFactory(jacksonObjectMapper().registerKotlinModule()),
            reindexService,
            repository,
            indexService,
        )

        task.runLongTask(
            from = null,
            param = """{"blockchain": "ETHEREUM", "target": "OWNERSHIP", "index":"ownership_test_index"}"""
        ).toList()

        coVerify {
            reindexService.reindex(BlockchainDto.ETHEREUM,
                OwnershipTaskParam.Target.OWNERSHIP,
                "ownership_test_index",
                null)
        }
    }

    @Test
    fun `should launch next run of the task`(): Unit = runBlocking {
        val task = OwnershipTask(
            OwnershipReindexProperties(
                enabled = true,
                blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
            ),
            ParamFactory(jacksonObjectMapper().registerKotlinModule()),
            reindexService,
            repository,
            indexService,
        )

        task.runLongTask(
            from = "ETHEREUM:cursor_1",
            param = """{"blockchain": "ETHEREUM", "target": "OWNERSHIP", "index":"ownership_test_index"}"""
        ).toList()

        coVerify {
            reindexService.reindex(BlockchainDto.ETHEREUM,
                OwnershipTaskParam.Target.OWNERSHIP,
                "ownership_test_index",
                "ETHEREUM:cursor_1")
        }
    }
}
