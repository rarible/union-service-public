package com.rarible.protocol.union.enrichment.custom.collection.updater

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.producer.UnionInternalOwnershipEventProducer
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnership
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class CustomCollectionOwnershipUpdaterTest {

    @MockK
    lateinit var ownershipService: OwnershipService

    @MockK
    lateinit var router: BlockchainRouter<OwnershipService>

    @MockK
    lateinit var eventProducer: UnionInternalOwnershipEventProducer

    @InjectMockKs
    lateinit var updater: CustomCollectionOwnershipUpdater

    @BeforeEach
    fun beforeEach() {
        clearMocks(router, ownershipService)
        every { router.getService(BlockchainDto.ETHEREUM) } returns ownershipService
        coEvery { eventProducer.sendChangeEvents(any()) } returns Unit
    }

    @Test
    fun `update - ok`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomUnionItem(itemId)
        val ownership1 = randomUnionOwnership(itemId)
        val ownership2 = randomUnionOwnership(itemId)
        val ownership3 = randomUnionOwnership(itemId)

        mockkGetOwnerships(itemId, null, "1", ownership1, ownership2)
        mockkGetOwnerships(itemId, "1", "2", ownership3)
        mockkGetOwnerships(itemId, "2", null)

        updater.update(item)

        coVerify(exactly = 1) { eventProducer.sendChangeEvents(listOf(ownership1.id, ownership2.id)) }
        coVerify(exactly = 1) { eventProducer.sendChangeEvents(listOf(ownership3.id)) }
    }

    private fun mockkGetOwnerships(
        itemId: ItemIdDto,
        continuation: String? = null,
        returnContinuation: String? = null,
        vararg ownerships: UnionOwnership
    ) {

        coEvery {
            ownershipService.getOwnershipsByItem(
                itemId = itemId.value,
                continuation = continuation,
                size = any()
            )
        } returns Page(0, returnContinuation, ownerships.toList())
    }
}
