package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.asyncBatchHandle
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.model.UnionTraitEvent
import com.rarible.protocol.union.enrichment.converter.TraitConverter
import com.rarible.protocol.union.enrichment.event.EnrichmentKafkaEventFactory
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.ItemAttributeCountChange
import com.rarible.protocol.union.enrichment.model.Trait
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.TraitRepository
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

@Component
class TraitService(
    private val itemRepository: ItemRepository,
    private val traitRepository: TraitRepository,
    private val producer: RaribleKafkaProducer<UnionTraitEvent>
) {
    suspend fun recalculateTraits(collectionId: EnrichmentCollectionId) {
        traitRepository.deleteAllByCollection(collectionId)
        logger.info("Recalculate traits for collection: $collectionId")
        val traits = itemRepository.getTraitsByCollection(collectionId = collectionId)
        traits.chunked(1000).forEach { chunk ->
            traitRepository.saveAll(chunk)
            sendChanged(chunk)
        }
        logger.info("Recalculated traits for collection: $collectionId")
    }

    suspend fun deleteWithZeroItemsCount() {
        logger.info("Deleting traits with zero items count")
        val deleted = AtomicInteger(0)
        val time = measureTimeMillis {
            deleted.set(traitRepository.deleteWithZeroItemsCount()
                .map { sendChanged(it) }
                .count())
        }
        logger.info("Deleted traits with zero items count: ${deleted.get()} time: ${time}ms")
    }

    suspend fun deleteAll(collectionId: EnrichmentCollectionId) {
        val traits = traitRepository.traitsByCollection(collectionId).toList()
        traits.map { it.copy(itemsCount = 0) }.chunked(1000).forEach { chunk ->
            sendChanged(chunk)
        }
        traitRepository.deleteAllByCollection(collectionId)
    }

    suspend fun changeItemsCount(
        collectionId: EnrichmentCollectionId,
        changes: Set<ItemAttributeCountChange>,
    ) {
        changes.asyncBatchHandle(TRAIT_HANDLE_BATCH) {
            val traitId = traitRepository.incrementItemsCount(
                collectionId,
                it.attribute,
                incTotal = it.totalChange,
                incListed = it.listedChange
            )
            indexTrait(traitId)
        }
    }

    private suspend fun indexTrait(traitId: String) {
        val event = traitRepository.get(traitId)?.let { TraitConverter.toEvent(it) }
        event?.let {
            producer.send(
                EnrichmentKafkaEventFactory.itemChangeEvent(it)
            ).ensureSuccess()
        }
    }

    suspend fun sendChanged(trait: Trait?) {
        trait?.let { TraitConverter.toEvent(it) }?.let {
            producer.send(
                EnrichmentKafkaEventFactory.itemChangeEvent(it)
            ).ensureSuccess()
        }
    }

    suspend fun sendChanged(chunk: List<Trait>) {
        val sent = producer.send(chunk.map {
            EnrichmentKafkaEventFactory.itemChangeEvent(TraitConverter.toEvent(it))
        }).count()
        logger.info("Sent $sent recalculated traits")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(TraitService::class.java)

        // We don't expect that there will be more than 500 traits in one collection
        const val TRAIT_HANDLE_BATCH = 500
    }
}
