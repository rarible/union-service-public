package com.rarible.protocol.union.worker.task.search.trait

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsTraitConverter.toEsTrait
import com.rarible.protocol.union.core.service.router.ActiveBlockchainProvider
import com.rarible.protocol.union.core.task.TraitTaskParam
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.TraitConverter
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.Trait
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.TraitRepository
import com.rarible.protocol.union.enrichment.repository.search.EsTraitRepository
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.worker.config.TraitReindexProperties
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.ParamFactory
import com.rarible.protocol.union.worker.test.randomTrait
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.mockk
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class TraitTaskTest {
    private val traitReindexProperties = TraitReindexProperties(enabled = true)
    private val paramFactory = ParamFactory(jacksonObjectMapper().registerKotlinModule())
    private val featureFlags = FeatureFlagsProperties()
    private val esTraitRepository = mockk<EsTraitRepository> {
        coEvery {
            bulk(any(), any(), any(), any())
        } answers { }
    }
    private val traitRepository = mockk<TraitRepository> {
        coEvery {
            saveAll(any())
        } answers { arg(0) }
    }
    private val collectionRepository = mockk<CollectionRepository>()
    private val searchTaskMetricFactory = SearchTaskMetricFactory(SimpleMeterRegistry())
    private val taskRepository = mockk<TaskRepository> {
        coEvery {
            findByTypeAndParam(any(), any())
        } returns mono { Task(type = "", param = "", running = true) }
    }

    @Test
    fun success() = runBlocking<Unit> {
        val task = TraitTask(
            traitReindexProperties,
            paramFactory,
            featureFlags,
            esTraitRepository,
            traitRepository,
            collectionRepository,
            searchTaskMetricFactory,
            taskRepository,
            ActiveBlockchainProvider.of(BlockchainDto.ETHEREUM)
        )
        val param = paramFactory.toString(
            TraitTaskParam(
                versionData = 1,
                settingsHash = "",
                blockchain = BlockchainDto.ETHEREUM,
                index = "test_index"
            )
        )

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
            traitRepository.traitsByCollection(collection.id)
        } returns listOf(trait).asFlow()

        task.runLongTask(null, param).toList()

        coVerifyAll {
            collectionRepository.findAll(null, BlockchainDto.ETHEREUM, any())
            collectionRepository.findAll(collection.id, BlockchainDto.ETHEREUM, any())
            traitRepository.traitsByCollection(collection.id)
            esTraitRepository.bulk(listOf(TraitConverter.toEvent(trait).toEsTrait()), emptyList(), any(), any())
        }
    }
}
