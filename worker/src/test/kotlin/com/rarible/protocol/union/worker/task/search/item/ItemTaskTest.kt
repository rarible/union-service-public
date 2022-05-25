package com.rarible.protocol.union.worker.task.search.item

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.union.api.client.ItemControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.worker.config.BlockchainReindexProperties
import com.rarible.protocol.union.worker.config.CollectionReindexProperties
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono
import java.math.BigInteger
import java.time.Instant

internal class ItemTaskTest {

    private val repo = mockk<EsItemRepository> {
        coEvery {
            saveAll(any())
        } answers { arg(0) }
    }

    private val ethItem = ItemDto(
        id = ItemIdDto(BlockchainDto.ETHEREUM, "${randomAddress()}", randomBigInt()),
        collection = CollectionIdDto(BlockchainDto.ETHEREUM, "${randomAddress()}"),
        blockchain = BlockchainDto.ETHEREUM,
        deleted = false,
        sellers = 1,
        mintedAt = Instant.now(),
        lastUpdatedAt = Instant.now(),
        lazySupply = BigInteger.ZERO,
        supply = BigInteger.ONE
    )
    private val flowItem = ItemDto(
        id = ItemIdDto(BlockchainDto.FLOW, "${randomAddress()}", randomBigInt()),
        collection = CollectionIdDto(BlockchainDto.FLOW, "${randomAddress()}"),
        blockchain = BlockchainDto.FLOW,
        deleted = false,
        sellers = 1,
        mintedAt = Instant.now(),
        lastUpdatedAt = Instant.now(),
        lazySupply = BigInteger.ZERO,
        supply = BigInteger.ONE
    )

    private val ethContinuation = "${ethItem.lastUpdatedAt.toEpochMilli()}_${ethItem.id}"
    private val flowContinuation = "${flowItem.lastUpdatedAt.toEpochMilli()}_${flowItem.id}"
    private val firstCombinedContinuation = CombinedContinuation(
        mapOf(
            BlockchainDto.ETHEREUM.toString() to ethContinuation,
            BlockchainDto.FLOW.toString() to flowContinuation,
        )
    )

    private val completedContinuation = CombinedContinuation(
        mapOf(
            BlockchainDto.ETHEREUM.toString() to ArgSlice.COMPLETED,
            BlockchainDto.FLOW.toString() to ArgSlice.COMPLETED,
        )
    )

    private val client = mockk<ItemControllerApi> {
        every { getAllItems(any<List<BlockchainDto>>(), null, any(), any(), any(), any()) } returns ItemsDto(
            total = 1L,
            items = listOf(ethItem, flowItem),
            continuation = firstCombinedContinuation.toString()
        ).toMono()

        every {
            getAllItems(
                any<List<BlockchainDto>>(),
                firstCombinedContinuation.toString(),
                any(),
                any(),
                any(),
                any()
            )
        } returns ItemsDto(
            total = 0L,
            items = emptyList(),
            continuation = completedContinuation.toString()
        ).toMono()
    }

    @Test
    internal fun `should start first task`() {
        runBlocking {
            val task = ItemTask(
                CollectionReindexProperties(
                    enabled = true,
                    blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
                ),
                client,
                repo
            )
            task.runLongTask(null, "ETHEREUM").toList()

            coVerifyAll {
                client.getAllItems(
                    listOf(BlockchainDto.ETHEREUM),
                    null,
                    1000,
                    true,
                    Long.MIN_VALUE,
                    Long.MAX_VALUE
                )

                repo.saveAll(any())

                client.getAllItems(
                    listOf(BlockchainDto.ETHEREUM),
                    firstCombinedContinuation.toString(),
                    1000,
                    true,
                    Long.MIN_VALUE,
                    Long.MAX_VALUE
                )
            }
        }
    }
}