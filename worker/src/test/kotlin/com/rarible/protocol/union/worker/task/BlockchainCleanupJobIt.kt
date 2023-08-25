package com.rarible.protocol.union.worker.task

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.core.converter.EsCollectionConverter
import com.rarible.protocol.union.core.converter.EsItemConverter.toEsItem
import com.rarible.protocol.union.core.converter.EsOwnershipConverter
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.CollectionDtoConverter
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityDtoConverter
import com.rarible.protocol.union.enrichment.converter.ItemDtoConverter
import com.rarible.protocol.union.enrichment.converter.OwnershipDtoConverter
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentMintActivity
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnership
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.integration.ethereum.data.randomPolygonCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomPolygonItemId
import com.rarible.protocol.union.integration.ethereum.data.randomPolygonOwnershipId
import com.rarible.protocol.union.worker.IntegrationTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class BlockchainCleanupJobIt {

    @Autowired
    lateinit var collectionRepository: CollectionRepository

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    lateinit var activityRepository: ActivityRepository

    @Autowired
    lateinit var esCollectionRepository: EsCollectionRepository

    @Autowired
    lateinit var esItemRepository: EsItemRepository

    @Autowired
    lateinit var esOwnershipRepository: EsOwnershipRepository

    @Autowired
    lateinit var esActivityRepository: EsActivityRepository

    @Autowired
    lateinit var esActivityConverter: EsActivityConverter

    @Autowired
    lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @Autowired
    lateinit var job: BlockchainCleanupJob

    @BeforeEach
    fun beforeEach() {
        elasticsearchTestBootstrapper.bootstrap()
        esActivityRepository.init()
        esOwnershipRepository.init()
        esItemRepository.init()
        esCollectionRepository.init()
    }

    @Test
    fun `cleanup - ok, collections`() = runBlocking {
        val ethCollection = collectionRepository.save(randomEnrichmentCollection(randomEthCollectionId()))
        val ethEsCollection = EsCollectionConverter.convert(CollectionDtoConverter.convert(ethCollection))
        val polyCollection = collectionRepository.save(randomEnrichmentCollection(randomPolygonCollectionId()))
        val polyEsCollection = EsCollectionConverter.convert(CollectionDtoConverter.convert(polyCollection))

        esCollectionRepository.bulk(listOf(ethEsCollection, polyEsCollection))

        Wait.waitAssert {
            assertThat(esCollectionRepository.findById(ethEsCollection.collectionId)).isNotNull
            assertThat(esCollectionRepository.findById(polyEsCollection.collectionId)).isNotNull
        }

        job.handle(null, BlockchainDto.POLYGON.name).collect()

        Wait.waitAssert {
            assertThat(collectionRepository.get(ethCollection.id)).isNotNull
            assertThat(collectionRepository.get(polyCollection.id)).isNull()
            assertThat(esCollectionRepository.findById(ethEsCollection.collectionId)).isNotNull
            assertThat(esCollectionRepository.findById(polyCollection.collectionId)).isNull()
        }

        job.handle(null, BlockchainDto.ETHEREUM.name).collect()

        Wait.waitAssert {
            assertThat(collectionRepository.get(ethCollection.id)).isNull()
            assertThat(esCollectionRepository.findById(ethEsCollection.collectionId)).isNull()
        }
    }

    @Test
    fun `cleanup - ok, items`() = runBlocking {
        val ethItem = itemRepository.save(randomShortItem(randomEthItemId()))
        val ethEsItem = ItemDtoConverter.convert(randomUnionItem(ethItem.id.toDto())).toEsItem()
        val polyItem = itemRepository.save(randomShortItem(randomPolygonItemId()))
        val polyEsItem = ItemDtoConverter.convert(randomUnionItem(polyItem.id.toDto())).toEsItem()

        esItemRepository.bulk(listOf(ethEsItem, polyEsItem))

        Wait.waitAssert {
            assertThat(esItemRepository.findById(ethEsItem.id)).isNotNull
            assertThat(esItemRepository.findById(polyEsItem.id)).isNotNull
        }

        job.handle(null, BlockchainDto.POLYGON.name).collect()

        Wait.waitAssert {
            assertThat(itemRepository.get(ethItem.id)).isNotNull
            assertThat(itemRepository.get(polyItem.id)).isNull()

            assertThat(esItemRepository.findById(ethEsItem.id)).isNotNull
            assertThat(esItemRepository.findById(polyEsItem.id)).isNull()
        }

        job.handle(null, BlockchainDto.ETHEREUM.name).collect()

        Wait.waitAssert {
            assertThat(itemRepository.get(ethItem.id)).isNull()
            assertThat(esItemRepository.findById(ethEsItem.id)).isNull()
        }
    }

    @Test
    fun `cleanup - ok, ownerships`() = runBlocking {
        val ethOwnership = ownershipRepository.save(randomShortOwnership(randomEthOwnershipId()))
        val ethOwnershipDto = OwnershipDtoConverter.convert(randomUnionOwnership(ethOwnership.id.toDto()))
        val ethEsOwnership = EsOwnershipConverter.convert(ethOwnershipDto)
        val polyOwnership = ownershipRepository.save(randomShortOwnership(randomPolygonOwnershipId()))
        val polyOwnershipDto = OwnershipDtoConverter.convert(randomUnionOwnership(polyOwnership.id.toDto()))
        val polyEsOwnership = EsOwnershipConverter.convert(polyOwnershipDto)

        esOwnershipRepository.bulk(listOf(ethEsOwnership, polyEsOwnership))

        Wait.waitAssert {
            assertThat(esOwnershipRepository.findById(ethEsOwnership.ownershipId)).isNotNull
            assertThat(esOwnershipRepository.findById(polyEsOwnership.ownershipId)).isNotNull
        }

        job.handle(null, BlockchainDto.POLYGON.name).collect()

        Wait.waitAssert {
            assertThat(ownershipRepository.get(ethOwnership.id)).isNotNull
            assertThat(ownershipRepository.get(polyOwnership.id)).isNull()

            assertThat(esOwnershipRepository.findById(ethEsOwnership.ownershipId)).isNotNull
            assertThat(esOwnershipRepository.findById(polyEsOwnership.ownershipId)).isNull()
        }

        job.handle(null, BlockchainDto.ETHEREUM.name).collect()

        Wait.waitAssert {
            assertThat(ownershipRepository.get(ethOwnership.id)).isNull()
            assertThat(esOwnershipRepository.findById(ethEsOwnership.ownershipId)).isNull()
        }
    }

    @Test
    fun `cleanup - ok, activities`() = runBlocking {
        val ethActivity = activityRepository.save(randomEnrichmentMintActivity(randomEthItemId()))
        val ethEsActivity = esActivityConverter.convert(EnrichmentActivityDtoConverter.convert(ethActivity), null)!!
        val polyActivity = activityRepository.save(randomEnrichmentMintActivity(randomPolygonItemId()))
        val polyEsActivity = esActivityConverter.convert(EnrichmentActivityDtoConverter.convert(polyActivity), null)!!

        esActivityRepository.bulk(listOf(ethEsActivity, polyEsActivity))

        Wait.waitAssert {
            assertThat(esActivityRepository.findById(ethEsActivity.activityId)).isNotNull
            assertThat(esActivityRepository.findById(polyEsActivity.activityId)).isNotNull
        }

        job.handle(null, BlockchainDto.POLYGON.name).collect()

        Wait.waitAssert {
            assertThat(activityRepository.get(ethActivity.id)).isNotNull
            assertThat(activityRepository.get(polyActivity.id)).isNull()

            assertThat(esActivityRepository.findById(ethEsActivity.activityId)).isNotNull
            assertThat(esActivityRepository.findById(polyEsActivity.activityId)).isNull()
        }

        job.handle(null, BlockchainDto.ETHEREUM.name).collect()

        Wait.waitAssert {
            assertThat(activityRepository.get(ethActivity.id)).isNull()
            assertThat(esActivityRepository.findById(ethEsActivity.activityId)).isNull()
        }
    }
}
