package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.core.model.ReconciliationMarkType
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.ReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.listener.test.data.randomCollectionMark
import com.rarible.protocol.union.listener.test.data.randomItemMark
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class ReconciliationMarkJobIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var itemReconciliationMarkRepository: ReconciliationMarkRepository

    private val refreshService: EnrichmentRefreshService = mockk()

    lateinit var jobHandler: ReconciliationMarkJobHandler

    private val itemEvent: ItemEventDto = mockk()

    private val collectionEvent: CollectionEventDto = mockk()

    @BeforeEach
    fun beforeEach() {
        jobHandler = ReconciliationMarkJobHandler(
            itemReconciliationMarkRepository,
            refreshService,
            listOf(BlockchainDto.ETHEREUM)
        )
        clearMocks(refreshService)
    }

    @Test
    fun `reconcile collections`() = runBlocking<Unit> {
        val collectionMarks = (1..50).map { randomCollectionMark() }

        collectionMarks.forEach { itemReconciliationMarkRepository.save(it) }
        val saved = itemReconciliationMarkRepository.findByType(ReconciliationMarkType.COLLECTION, 100)

        assertThat(saved).hasSize(collectionMarks.size)
        coEvery { refreshService.reconcileCollection(any()) } returns collectionEvent

        jobHandler.handle()
        val remain = itemReconciliationMarkRepository.findByType(ReconciliationMarkType.COLLECTION, 100)

        coVerify(exactly = collectionMarks.size) { refreshService.reconcileCollection(any()) }
        assertThat(remain).hasSize(0)
    }

    @Test
    fun `reconcile collection - with fail`() = runBlocking<Unit> {
        val collectionMarks = (1..10).map { randomCollectionMark() }
        val failedCollectionId = IdParser.parseCollectionId(collectionMarks[5].id)

        collectionMarks.forEach { itemReconciliationMarkRepository.save(it) }

        coEvery { refreshService.reconcileCollection(any()) } returns collectionEvent
        coEvery { refreshService.reconcileCollection(failedCollectionId) } throws RuntimeException()

        jobHandler.handle()

        // 1 additional call for single retry for one corrupted item
        ///coVerify(exactly = collectionMarks.size + 1) { refreshService.reconcileCollection(any()) }
        // One failed item mark should remain in DB
        val failedMarks = itemReconciliationMarkRepository.findByType(ReconciliationMarkType.COLLECTION, 100)
        assertThat(failedMarks).hasSize(1)
        assertThat(failedMarks[0].retries).isEqualTo(2)
    }

    @Test
    fun `reconcile items`() = runBlocking<Unit> {
        val itemMarks = (1..50).map { randomItemMark() }

        itemMarks.forEach { itemReconciliationMarkRepository.save(it) }
        val saved = itemReconciliationMarkRepository.findByType(ReconciliationMarkType.ITEM, 100)

        assertThat(saved).hasSize(itemMarks.size)
        coEvery { refreshService.reconcileItem(any(), any()) } returns itemEvent

        jobHandler.handle()
        val remain = itemReconciliationMarkRepository.findByType(ReconciliationMarkType.ITEM, 100)

        coVerify(exactly = itemMarks.size) { refreshService.reconcileItem(any(), false) }
        assertThat(remain).hasSize(0)
    }

    @Test
    fun `reconcile items - with fail`() = runBlocking<Unit> {
        val itemMarks = (1..10).map { randomItemMark() }
        val failedItemId = IdParser.parseItemId(itemMarks[5].id)

        itemMarks.forEach { itemReconciliationMarkRepository.save(it) }

        coEvery { refreshService.reconcileItem(any(), any()) } returns itemEvent
        coEvery { refreshService.reconcileItem(failedItemId, false) } throws RuntimeException()

        jobHandler.handle()

        // 1 additional call for single retry for one corrupted item
        coVerify(exactly = itemMarks.size + 1) { refreshService.reconcileItem(any(), false) }
        // One failed item mark should remain in DB
        val failedMarks = itemReconciliationMarkRepository.findByType(ReconciliationMarkType.ITEM, 100)
        assertThat(failedMarks).hasSize(1)
        assertThat(failedMarks[0].retries).isEqualTo(2)
    }
}
