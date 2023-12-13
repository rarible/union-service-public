package com.rarible.protocol.union.worker.job.sync

import com.rarible.protocol.union.core.converter.EsTraitConverter.toEsTrait
import com.rarible.protocol.union.core.model.elastic.EsTrait
import com.rarible.protocol.union.core.task.SyncTraitJobParam
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.converter.TraitConverter
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.Trait
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.TraitRepository
import com.rarible.protocol.union.enrichment.repository.search.EsTraitRepository
import com.rarible.protocol.union.worker.task.search.EsRateLimiter
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Component

@Component
class SyncTraitJob(
    private val traitRepository: TraitRepository,
    private val itemRepository: ItemRepository,
    private val collectionRepository: CollectionRepository,
    private val esTraitRepository: EsTraitRepository,
    esRateLimiter: EsRateLimiter
) : AbstractSyncJob<Trait, EsTrait, SyncTraitJobParam>(
    "Trait",
    SyncTraitJobParam::class.java,
    esRateLimiter
) {
    override suspend fun getNext(param: SyncTraitJobParam, state: String?): Slice<Trait> {
        val (collectionIds, nextState) = if (!param.collectionId.isNullOrBlank()) {
            Pair(listOf(EnrichmentCollectionId.of(param.collectionId!!)), null)
        } else {
            val collectionIds = collectionRepository.findAll(
                fromIdExcluded = state?.let { EnrichmentCollectionId.of(it) },
                blockchain = param.blockchain,
                limit = COLLECTION_BATCH_SIZE
            ).map { it.id }.toList()
            Pair(collectionIds, collectionIds.lastOrNull()?.toString())
        }
        val traits = collectionIds.flatMap { collectionId ->
            val actualTraits = itemRepository.getTraitsByCollection(collectionId = collectionId)
            val actualTraitIds = actualTraits.map { it.id }.toSet()
            val staleTraits = traitRepository.traitsByCollection(collectionId = collectionId)
                .filter { !actualTraitIds.contains(it.id) }
                .map { it.copy(itemsCount = 0, listedItemsCount = 0) }
                .toList()
            actualTraits + staleTraits
        }
        return Slice(nextState, traits.toList())
    }

    override suspend fun updateDb(param: SyncTraitJobParam, unionEntities: List<Trait>): List<EsTrait> {
        traitRepository.saveAll(unionEntities)
        return unionEntities.map { TraitConverter.toEvent(it).toEsTrait() }
    }

    override suspend fun updateEs(
        param: SyncTraitJobParam,
        enrichmentEntities: List<EsTrait>,
        unionEntities: List<Trait>
    ) {
        val toSave = enrichmentEntities.filter { it.itemsCount > 0 }
        val toDelete = enrichmentEntities.filter { it.itemsCount <= 0 }.map { it.id }
        esTraitRepository.bulk(
            entitiesToSave = toSave,
            idsToDelete = toDelete,
            refreshPolicy = WriteRequest.RefreshPolicy.NONE
        )
    }

    override suspend fun notify(
        param: SyncTraitJobParam,
        enrichmentEntities: List<EsTrait>,
        unionEntities: List<Trait>
    ) {
        // do nothing
    }

    companion object {
        const val COLLECTION_BATCH_SIZE = 10
    }
}
