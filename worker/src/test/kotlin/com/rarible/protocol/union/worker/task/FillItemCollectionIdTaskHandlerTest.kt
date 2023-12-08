package com.rarible.protocol.union.worker.task

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import scalether.domain.Address

@ExtendWith(MockKExtension::class)
internal class FillItemCollectionIdTaskHandlerTest {
    @InjectMockKs
    private lateinit var fillItemCollectionIdTaskHandler: FillItemCollectionIdTaskHandler

    @MockK
    private lateinit var itemRepository: ItemRepository

    @MockK
    private lateinit var enrichmentItemService: EnrichmentItemService

    @Test
    fun run() = runBlocking<Unit> {
        val fromId = ShortItemId(randomEthItemId())
        val item1 = randomShortItem().copy(blockchain = BlockchainDto.SOLANA)
        val item2 = randomShortItem()
        val item3 = randomShortItem()
        val item2ForUpdate = randomShortItem().copy(itemId = "${Address.ONE()}:1")
        val item3ForUpdate = randomShortItem().copy(itemId = "${Address.TWO()}:1")
        coEvery { itemRepository.findAll(fromIdExcluded = fromId) } returns flowOf(item1, item2, item3)
        coEvery { itemRepository.get(item2.id) } returns item2ForUpdate
        coEvery { itemRepository.get(item3.id) } returns item3ForUpdate
        coEvery { enrichmentItemService.resolveCustomCollection(item2ForUpdate) } returns CollectionIdDto(
            blockchain = BlockchainDto.ETHEREUM,
            value = Address.THREE().toString()
        )
        coEvery { enrichmentItemService.resolveCustomCollection(item3ForUpdate) } returns null

        coEvery {
            enrichmentItemService.save(
                item2ForUpdate.copy(
                    collectionId = Address.THREE().toString()
                )
            )
        } returns item2ForUpdate
        coEvery {
            enrichmentItemService.save(
                item3ForUpdate.copy(
                    collectionId = Address.TWO().toString()
                )
            )
        } returns item2ForUpdate

        val result = fillItemCollectionIdTaskHandler.runLongTask(from = fromId.toString(), param = "").toList()

        assertThat(result).containsExactly(item1.id.toString(), item2.id.toString(), item3.id.toString())
    }
}
