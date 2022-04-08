package com.rarible.protocol.union.integration.tezos.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.tezos.dto.TezosCollectionSafeEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.converter.TezosCollectionConverter
import com.rarible.protocol.union.integration.tezos.data.randomTezosCollectionDto
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TezosCollectionEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<UnionCollectionEvent> = mockk()
    private val handler = TezosCollectionEventHandler(incomingEventHandler)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `ethereum collection event`() = runBlocking {
        val collection = randomTezosCollectionDto()
        val eventId = randomString()

        handler.handle(
            TezosCollectionSafeEventDto(
                TezosCollectionSafeEventDto.Type.UPDATE,
                eventId,
                collection.id,
                collection
            )
        )

        val unionCollection = TezosCollectionConverter.convert(collection, BlockchainDto.TEZOS)
        val expected = UnionCollectionUpdateEvent(unionCollection)

        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }
}