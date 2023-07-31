package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.core.model.blockchainAndIndexerMarks
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.data.randomImxCollection
import com.rarible.protocol.union.integration.immutablex.converter.ImxCollectionConverter
import com.rarible.protocol.union.integration.immutablex.model.ImxCollectionCreator
import com.rarible.protocol.union.integration.immutablex.repository.ImxCollectionCreatorRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ImxCollectionEventHandlerTest {

    private val collectionHandler: IncomingEventHandler<UnionCollectionEvent> = mockk {
        coEvery { onEvent(any()) } returns Unit
    }

    protected val collectionCreatorRepository: ImxCollectionCreatorRepository = mockk {
        coEvery { getAll(any()) } returns emptyList()
        coEvery { saveAll(any()) } returns Unit
    }

    private val collectionEventHandler = ImxCollectionEventHandler(
        collectionHandler, collectionCreatorRepository
    )

    private val blockchain = BlockchainDto.IMMUTABLEX

    @Test
    fun `on collection updated`() = runBlocking<Unit> {
        val imxCollection = randomImxCollection()

        val collection = ImxCollectionConverter.convert(imxCollection, blockchain)
        val creator = ImxCollectionCreator(imxCollection.address, imxCollection.projectOwnerAddress!!)

        collectionEventHandler.handle(listOf(imxCollection))

        val marks = blockchainAndIndexerMarks(imxCollection.updatedAt!!)
        coVerify(exactly = 1) { collectionHandler.onEvent(any()) }
        coVerify(exactly = 1) { collectionHandler.onEvent(UnionCollectionUpdateEvent(collection, marks)) }

        coVerify(exactly = 1) { collectionCreatorRepository.saveAll(listOf(creator)) }
    }

    @Test
    fun `on collection updated - without creator`() = runBlocking<Unit> {
        val imxCollection = randomImxCollection(projectOwnerAddress = null)

        val collection = ImxCollectionConverter.convert(imxCollection, blockchain)

        collectionEventHandler.handle(listOf(imxCollection))

        val marks = blockchainAndIndexerMarks(imxCollection.updatedAt!!)
        coVerify(exactly = 1) { collectionHandler.onEvent(any()) }
        coVerify(exactly = 1) { collectionHandler.onEvent(UnionCollectionUpdateEvent(collection, marks)) }

        coVerify(exactly = 1) { collectionCreatorRepository.saveAll(emptyList()) }
    }
}
