package com.rarible.protocol.union.worker.task

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.configuration.CustomCollectionMapping
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCollectionProperties
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaService
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.worker.IntegrationTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.web3j.crypto.Keys
import scalether.domain.Address

@IntegrationTest
class ArtBlocksCollectionProjectJobIt {

    private val artBlocksCollectionId = EnrichmentCollectionId(randomEthCollectionId())
    private val artBlocksMapping = CustomCollectionMapping(
        name = "artblocks",
        collections = listOf(artBlocksCollectionId.toString())
    )
    val properties = EnrichmentCollectionProperties(mappings = listOf(artBlocksMapping))

    val collectionMetaService: CollectionMetaService = mockk {
        coEvery { schedule(any(), any(), any(), any()) } returns Unit
    }

    @Autowired
    lateinit var template: ReactiveMongoTemplate

    @Test
    fun `extra updated - ok`() = runBlocking<Unit> {
        val sub1 = createCollection(getSubCollectionId(artBlocksCollectionId, 0).collectionId)
        val sub2 = createCollection(getSubCollectionId(artBlocksCollectionId, 1).collectionId)
        val sub3 = createCollection(getSubCollectionId(artBlocksCollectionId, 5).collectionId)
        val other = createCollection(randomEthAddress(), withParent = false)

        val job = ArtBlocksCollectionProjectJob(properties, collectionMetaService, template)
        job.handle(null, "").collect()

        assertThat(find(sub1.id).extra["project_id"]).isEqualTo("0")
        assertThat(find(sub2.id).extra["project_id"]).isEqualTo("1")
        assertThat(find(sub3.id).extra["project_id"]).isEqualTo("5")
        assertThat(find(other.id).extra).isEmpty()
    }

    private suspend fun createCollection(collectionId: String, withParent: Boolean = true): EnrichmentCollection {
        val collection = randomEnrichmentCollection(CollectionIdDto(BlockchainDto.ETHEREUM, collectionId))
            .copy(parent = if (withParent) artBlocksCollectionId else null)
        return template.save(collection).awaitSingle()
    }

    private fun getSubCollectionId(parent: EnrichmentCollectionId, projectId: Int): EnrichmentCollectionId {
        val token = Address.apply(
            Keys.getAddress("custom_collection:artblocks:${parent.collectionId}:$projectId")
        ).prefixed()
        return EnrichmentCollectionId(parent.blockchain, token)
    }

    private suspend fun find(id: EnrichmentCollectionId): EnrichmentCollection {
        return template.findById(id, EnrichmentCollection::class.java).awaitSingleOrNull()!!
    }
}
