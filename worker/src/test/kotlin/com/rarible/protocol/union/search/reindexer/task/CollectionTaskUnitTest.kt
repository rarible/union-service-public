package com.rarible.protocol.union.search.reindexer.task

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.api.client.CollectionControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.continuation.CollectionContinuation
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.worker.task.CollectionTask
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono

@Suppress("ReactiveStreamsUnusedPublisher")
@Disabled("investigate test failure on jenkins")
class CollectionTaskUnitTest {

    private val collection = CollectionDto(
        id = CollectionIdDto(BlockchainDto.ETHEREUM, "${randomAddress()}"),
        blockchain = BlockchainDto.ETHEREUM,
        type = CollectionDto.Type.ERC721,
        name = randomString(),
    )

    private val repo = mockk<EsCollectionRepository> {
        coEvery {
            saveAll(any())
        } answers { arg(0) }
    }


    private val client = mockk<CollectionControllerApi> {
        every { getAllCollections(any<List<BlockchainDto>>(), null, any()) } returns CollectionsDto(
            total = 1L,
            collections = listOf(
                collection
            ),
            continuation = CollectionContinuation.ById.getContinuation(collection).toString()
        ).toMono()

        every {
            getAllCollections(any<List<BlockchainDto>>(),
                CollectionContinuation.ById.getContinuation(collection).toString(),
                any())
        } returns CollectionsDto(
            total = 0L,
            collections = emptyList(),
            continuation = null
        ).toMono()
    }

    @Test
    internal fun `should start first task`() {
        runBlocking {
            val task = CollectionTask(
                listOf(BlockchainDto.ETHEREUM),
                true,
                client, repo
            )

            task.runLongTask(null, "COLLECTION_REINDEX_ETHEREUM").toList()

            coVerifyAll {
                client.getAllCollections(
                    listOf(BlockchainDto.ETHEREUM),
                    null,
                    1000
                )

                repo.saveAll(any())
            }
        }
    }

    @Test
    internal fun `should return empty continuation`() {
        runBlocking {
            val task = CollectionTask(
                listOf(BlockchainDto.ETHEREUM),
                true,
                client, repo
            )

            val from = CollectionContinuation.ById.getContinuation(collection).toString()
            val list = task.runLongTask(from, "COLLECTION_REINDEX_ETHEREUM").toList()


            verifyAll {
                client.getAllCollections(
                    listOf(BlockchainDto.ETHEREUM),
                    from,
                    1000
                )
            }
            assertThat(list).isEqualTo(listOf(""))
        }
    }
}
