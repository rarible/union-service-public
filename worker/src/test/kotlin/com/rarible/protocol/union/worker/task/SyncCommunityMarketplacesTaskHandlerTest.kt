package com.rarible.protocol.union.worker.task

import com.rarible.protocol.union.enrichment.model.Marketplace
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.worker.cmp.client.CommunityMarketplaceClient
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class SyncCommunityMarketplacesTaskHandlerTest {
    @InjectMockKs
    private lateinit var syncCommunityMarketplacesTaskHandler: SyncCommunityMarketplacesTaskHandler

    @MockK
    private lateinit var communityMarketplaceClient: CommunityMarketplaceClient

    @MockK
    private lateinit var collectionRepository: CollectionRepository

    @Test
    fun run() = runBlocking<Unit> {
        val collectionId1 = randomEnrichmentCollection().id
        val collectionId2 = randomEnrichmentCollection().id
        val collectionId3 = randomEnrichmentCollection().id
        val collectionId4 = randomEnrichmentCollection().id
        val marketplace1 =
            Marketplace(id = "1", collectionIds = setOf(collectionId1, collectionId2), metaRefreshPriority = 50)
        val marketplace2 =
            Marketplace(id = "2", collectionIds = setOf(collectionId3, collectionId4), metaRefreshPriority = null)

        coEvery { communityMarketplaceClient.getMarketplaces("from") } returns listOf(marketplace1)
        coEvery { communityMarketplaceClient.getMarketplaces("1") } returns listOf(marketplace2)
        coEvery { communityMarketplaceClient.getMarketplaces("2") } returns emptyList()

        coEvery {
            collectionRepository.updatePriority(
                collectionIds = setOf(collectionId1, collectionId2),
                priority = 50,
            )
        } returns Unit
        coEvery {
            collectionRepository.updatePriority(
                collectionIds = setOf(collectionId3, collectionId4),
                priority = null,
            )
        } returns Unit

        val result = syncCommunityMarketplacesTaskHandler.runLongTask("from", "").toList()

        assertThat(result).containsExactly("1", "2")
    }
}
