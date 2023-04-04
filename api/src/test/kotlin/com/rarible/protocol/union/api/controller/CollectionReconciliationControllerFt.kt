package com.rarible.protocol.union.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import java.time.Instant

@FlowPreview
@IntegrationTest
internal class CollectionReconciliationControllerFt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var collectionRepository: CollectionRepository

    @Test
    fun getCollections() {
        val collectionDto1 = randomEthCollectionDto(randomAddress())
        val collectionDto2 = randomEthCollectionDto(randomAddress())
        val enrichmentCollection1 =
            runBlocking {
                collectionRepository.save(
                    EnrichmentCollectionConverter.convert(
                        collection = EthCollectionConverter.convert(
                            collectionDto1,
                            BlockchainDto.ETHEREUM
                        )
                    ).copy(lastUpdatedAt = Instant.ofEpochMilli(2000))
                )
            }
        val enrichmentCollection2 =
            runBlocking {
                collectionRepository.save(
                    EnrichmentCollectionConverter.convert(
                        collection = EthCollectionConverter.convert(
                            collectionDto2,
                            BlockchainDto.ETHEREUM
                        )
                    ).copy(lastUpdatedAt = Instant.ofEpochMilli(3000))
                )
            }
        coEvery {
            testEthereumCollectionApi.getNftCollectionsByIds(
                match { request ->
                    request.ids.size == 2 &&
                        request.ids.contains(enrichmentCollection1.id.collectionId) &&
                        request.ids.contains(enrichmentCollection2.id.collectionId)
                }
            )
        } returns NftCollectionsDto(total = 0, collections = listOf(collectionDto1, collectionDto2)).toMono()

        val result = testRestTemplate.getForObject(
            "$baseUri/reconciliation/collections?lastUpdatedFrom={from}&lastUpdatedTo={to}&size={size}",
            CollectionsDto::class.java,
            Instant.ofEpochMilli(1000),
            Instant.ofEpochMilli(4000),
            20
        )

        assertThat(result.collections.map { it.id.fullId() }).containsExactlyInAnyOrder(
            enrichmentCollection1.id.toDto().fullId(),
            enrichmentCollection2.id.toDto().fullId(),
        )
    }
}
