package com.rarible.protocol.union.worker.task.search.trait

import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsTraitConverter.toEsTrait
import com.rarible.protocol.union.core.model.elastic.EsTrait
import com.rarible.protocol.union.core.service.router.ActiveBlockchainProvider
import com.rarible.protocol.union.core.task.ItemTaskParam
import com.rarible.protocol.union.core.task.TraitTaskParam
import com.rarible.protocol.union.enrichment.converter.TraitConverter
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.TraitRepository
import com.rarible.protocol.union.enrichment.repository.search.EsTraitRepository
import com.rarible.protocol.union.worker.config.TraitReindexProperties
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.elasticsearch.action.support.WriteRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TraitTask(
    private val properties: TraitReindexProperties,
    private val paramFactory: ParamFactory,
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val esRepository: EsTraitRepository,
    private val traitRepository: TraitRepository,
    private val collectionRepository: CollectionRepository,
    private val searchTaskMetricFactory: SearchTaskMetricFactory,
    private val taskRepository: TaskRepository,
    private val activeBlockchainProvider: ActiveBlockchainProvider,
) : TaskHandler<String> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val type: String
        get() = EsTrait.ENTITY_DEFINITION.reindexTask

    override suspend fun isAbleToRun(param: String): Boolean {
        val blockchain = paramFactory.parse<ItemTaskParam>(param).blockchain
        return properties.enabled && activeBlockchainProvider.isActive(blockchain)
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val taskParam = paramFactory.parse<TraitTaskParam>(param)
        val blockchain = taskParam.blockchain
        val counter = searchTaskMetricFactory.createReindexTraitCounter()
        val refreshPolicy =
            if (featureFlagsProperties.enableTraitSaveImmediateToElasticSearch) {
                WriteRequest.RefreshPolicy.IMMEDIATE
            } else {
                WriteRequest.RefreshPolicy.NONE
            }
        var continuation = from
        return flow {
            do {
                val collections = collectionRepository.findAll(
                    fromIdExcluded = continuation?.let { EnrichmentCollectionId.of(it) },
                    blockchain = blockchain,
                    limit = BATCH_SIZE
                ).toList()
                if (collections.isEmpty()) {
                    return@flow
                }
                var processed = 0
                collections.forEach { collection ->
                    val traits = traitRepository.traitsByCollection(collection.id)
                        .map { trait -> TraitConverter.toEvent(trait).toEsTrait() }
                        .toList()
                    if (traits.isEmpty()) {
                        return@forEach
                    }
                    esRepository.bulk(
                        entitiesToSave = traits,
                        idsToDelete = emptyList(),
                        indexName = taskParam.index,
                        refreshPolicy = refreshPolicy
                    )
                    counter.increment(traits.size)
                    processed += traits.size
                }
                logger.info("Processed $processed traits at continuation $continuation")
                continuation = collections.last().id.toString()
                emit(continuation.orEmpty())
            } while (!continuation.isNullOrBlank())
        }.takeWhile { taskRepository.findByTypeAndParam(type, param).awaitSingleOrNull()?.running ?: false }
    }

    companion object {
        const val BATCH_SIZE: Int = 1000
    }
}
