package com.rarible.protocol.union.search.reindexer.task

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.config.BlockchainReindexProperties
import com.rarible.protocol.union.worker.config.CollectionReindexProperties
import com.rarible.protocol.union.worker.task.search.ParamFactory
import com.rarible.protocol.union.worker.task.search.collection.CollectionReindexService
import com.rarible.protocol.union.worker.task.search.collection.CollectionTask
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@Suppress("ReactiveStreamsUnusedPublisher")
class CollectionTaskUnitTest {

    private val paramFactory = ParamFactory(jacksonObjectMapper())

    private val service = mockk<CollectionReindexService> {
        coEvery {
            reindex(any(), "collection_test_index", any())
        } returns flowOf("next_cursor")
    }

    @Test
    fun `should launch first run of the task`(): Unit {
        runBlocking {
            val task = CollectionTask(
                CollectionReindexProperties(
                    enabled = true,
                    blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
                ),
                paramFactory,
                service,
            )

            task.runLongTask(
                null,
                """{"blockchain": "ETHEREUM", "index":"collection_test_index"}"""
            ).toList()

            coVerify {
                service.reindex(BlockchainDto.ETHEREUM, "collection_test_index", null)
            }
        }
    }

    @Test
    fun `should launch next run of the task`(): Unit = runBlocking {
        val task = CollectionTask(
            CollectionReindexProperties(
                enabled = true,
                blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
            ),
            paramFactory,
            service
        )

        task.runLongTask(
            "ETHEREUM:cursor_1",
            """{"blockchain": "ETHEREUM", "index":"collection_test_index"}"""
        ).toList()

        coVerify {
            service.reindex(BlockchainDto.ETHEREUM, "collection_test_index", "ETHEREUM:cursor_1")
        }
    }
}
