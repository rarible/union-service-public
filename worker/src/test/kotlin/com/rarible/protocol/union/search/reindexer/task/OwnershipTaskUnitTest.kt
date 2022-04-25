package com.rarible.protocol.union.search.reindexer.task

import com.rarible.protocol.union.api.client.OwnershipControllerApi
import com.rarible.protocol.union.core.converter.EsOwnershipConverter
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.worker.config.SearchReindexerConfiguration
import com.rarible.protocol.union.worker.config.SearchReindexerProperties
import com.rarible.protocol.union.worker.task.OwnershipTask
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import randomEsOwnership
import randomOwnershipId
import reactor.core.publisher.Mono

@Disabled
class OwnershipTaskUnitTest {

    private val repository = mockk<EsOwnershipRepository> {
        coEvery {
            saveAll(any<List<EsOwnership>>())
        } returns emptyList()
    }

    private val converter = mockk<EsOwnershipConverter> {
        every {
            convert(any<OwnershipDto>())
        } returns randomEsOwnership(randomOwnershipId(BlockchainDto.ETHEREUM))
    }

    private val client = mockk<OwnershipControllerApi> {
        every {
            getAllOwnerships(
                listOf(BlockchainDto.ETHEREUM),
                null,
                OwnershipTask.PAGE_SIZE,
            )
        } returns Mono.just(
            OwnershipsDto(
                OwnershipTask.PAGE_SIZE.toLong(),
                "ETHEREUM:cursor_1",
                listOf(mockk()),
            )
        )
        every {
            getAllOwnerships(
                listOf(BlockchainDto.ETHEREUM),
                "ETHEREUM:cursor_1",
                OwnershipTask.PAGE_SIZE,
            )
        } returns Mono.just(
            OwnershipsDto(
                OwnershipTask.PAGE_SIZE.toLong(),
                null,
                listOf(mockk()),
            )
        )
    }

    @Test
    fun `should launch first run of the task`() = runBlocking<Unit> {
        val task = OwnershipTask(
            SearchReindexerConfiguration(SearchReindexerProperties()),
            client,
            repository,
            converter,
        )

        task.runLongTask(null, "OWNERSHIP_ETHEREUM").toList()

        coVerify {
            client.getAllOwnerships(
                listOf(BlockchainDto.ETHEREUM),
                null,
                OwnershipTask.PAGE_SIZE,
            )

            converter.convert(any<OwnershipDto>())

            repository.saveAll(any())
        }
    }

    @Test
    fun `should launch next run of the task`(): Unit = runBlocking {
        val task = OwnershipTask(
            SearchReindexerConfiguration(SearchReindexerProperties()),
            client,
            repository,
            converter
        )

        task.runLongTask("ETHEREUM:cursor_1", "OWNERSHIP_ETHEREUM").toList()

        coVerify {
            client.getAllOwnerships(
                listOf(BlockchainDto.ETHEREUM),
                "ETHEREUM:cursor_1",
                OwnershipTask.PAGE_SIZE,
            )

            converter.convert(any())

            repository.saveAll(any())
        }
    }
}
