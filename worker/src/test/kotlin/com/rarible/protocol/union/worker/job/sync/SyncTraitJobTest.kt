package com.rarible.protocol.union.worker.job.sync

import com.rarible.protocol.union.core.converter.EsTraitConverter.toEsTrait
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.TraitConverter
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.Trait
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.TraitRepository
import com.rarible.protocol.union.enrichment.repository.search.EsTraitRepository
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.worker.task.search.EsRateLimiter
import com.rarible.protocol.union.worker.test.randomTrait
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.elasticsearch.action.support.WriteRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SyncTraitJobTest {
    @MockK
    lateinit var traitRepository: TraitRepository

    @MockK
    lateinit var itemRepository: ItemRepository

    @MockK
    lateinit var collectionRepository: CollectionRepository

    @MockK
    lateinit var esTraitRepository: EsTraitRepository

    @MockK
    lateinit var esRateLimiter: EsRateLimiter

    @InjectMockKs
    lateinit var job: SyncTraitJob

    @BeforeEach
    fun setUp() {
        clearMocks(traitRepository, itemRepository, collectionRepository, esTraitRepository, esRateLimiter)
        coEvery { esRateLimiter.waitIfNecessary(any()) } returns Unit
        coEvery { traitRepository.saveAll(any()) } returns Unit
        coEvery { esTraitRepository.bulk(any(), any(), any(), any()) } returns Unit
    }

    @Test
    fun `fresh run`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection()
        coEvery {
            collectionRepository.findAll(
                fromIdExcluded = null,
                blockchain = BlockchainDto.ETHEREUM,
                limit = any()
            )
        } returns listOf(collection).asFlow()
        coEvery {
            collectionRepository.findAll(
                fromIdExcluded = collection.id,
                blockchain = BlockchainDto.ETHEREUM,
                limit = any()
            )
        } returns emptyList<EnrichmentCollection>().asFlow()

        val trait = randomTrait()
        coEvery {
            itemRepository.getTraitsByCollection(collectionId = collection.id)
        } returns listOf(trait)
        coEvery {
            traitRepository.traitsByCollection(collectionId = collection.id)
        } returns emptyList<Trait>().asFlow()

        val param = """{
            "blockchain": "ETHEREUM",
            "scope": "ES"
        }"""
        job.handle(null, param).toList()

        coVerify {
            traitRepository.saveAll(listOf(trait))
            esTraitRepository.bulk(
                listOf(TraitConverter.toEvent(trait).toEsTrait()),
                emptyList(),
                null,
                WriteRequest.RefreshPolicy.NONE
            )
        }
    }

    @Test
    fun `delete stale save actual`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection()
        coEvery {
            collectionRepository.findAll(
                fromIdExcluded = null,
                blockchain = BlockchainDto.ETHEREUM,
                limit = any()
            )
        } returns listOf(collection).asFlow()
        coEvery {
            collectionRepository.findAll(
                fromIdExcluded = collection.id,
                blockchain = BlockchainDto.ETHEREUM,
                limit = any()
            )
        } returns emptyList<EnrichmentCollection>().asFlow()

        val actualTrait = randomTrait()
        coEvery {
            itemRepository.getTraitsByCollection(collectionId = collection.id)
        } returns listOf(actualTrait)
        val staleTrait = randomTrait()
        coEvery {
            traitRepository.traitsByCollection(collectionId = collection.id)
        } returns listOf(staleTrait).asFlow()

        val param = """{
            "blockchain": "ETHEREUM",
            "scope": "ES"
        }"""
        job.handle(null, param).toList()

        coVerify {
            traitRepository.saveAll(listOf(actualTrait, staleTrait.copy(itemsCount = 0, listedItemsCount = 0)))
            esTraitRepository.bulk(
                listOf(TraitConverter.toEvent(actualTrait).toEsTrait()),
                listOf(staleTrait.id),
                null,
                WriteRequest.RefreshPolicy.NONE
            )
        }
    }

    @Test
    fun `delete stale save actual by collection id`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection()
        val actualTrait = randomTrait()
        coEvery {
            itemRepository.getTraitsByCollection(collectionId = collection.id)
        } returns listOf(actualTrait)
        val staleTrait = randomTrait()
        coEvery {
            traitRepository.traitsByCollection(collectionId = collection.id)
        } returns listOf(staleTrait).asFlow()

        val param = """{
            "blockchain": "ETHEREUM",
            "scope": "ES",
            "collectionId": "${collection.id}"
        }"""
        job.handle(null, param).toList()

        coVerify {
            traitRepository.saveAll(listOf(actualTrait, staleTrait.copy(itemsCount = 0, listedItemsCount = 0)))
            esTraitRepository.bulk(
                listOf(TraitConverter.toEvent(actualTrait).toEsTrait()),
                listOf(staleTrait.id),
                null,
                WriteRequest.RefreshPolicy.NONE
            )
        }
    }
}
