package com.rarible.protocol.union.worker.job

import com.rarible.core.kafka.chunked
import com.rarible.protocol.union.enrichment.meta.item.MetaTrimmer
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

abstract class AbstractTrimJob<Entity> {

    fun trim(from: String?) = flow<String> {
        coroutineScope {
            getEntityFrom(from)
                .chunked(CHUNK_SIZE)
                .collect { entities ->
                    entities
                        .map { entity ->
                            async { trim(entity) }
                        }
                        .awaitAll()
                        .onEach { entityId -> emit(entityId) }
                }
        }
    }

    protected abstract fun getEntityFrom(from: String?): Flow<Entity>

    protected abstract suspend fun trim(entity: Entity): String

    companion object {
        private const val CHUNK_SIZE = 1000
    }
}

@Component
class TrimCollectionMetaJob(
    private val metaTrimmer: MetaTrimmer,
    private val collectionRepository: CollectionRepository,
) : AbstractTrimJob<EnrichmentCollection>() {

    override fun getEntityFrom(from: String?): Flow<EnrichmentCollection> {
        return collectionRepository.findAll(from?.let { EnrichmentCollectionId.of(it) })
    }

    override suspend fun trim(entity: EnrichmentCollection): String {
        val metaEntry = entity.metaEntry
        val trimmedData = metaTrimmer.trim(metaEntry?.data)
        if (trimmedData != metaEntry?.data) {
            val updatedCollection = entity.copy(metaEntry = metaEntry?.withData(trimmedData))
            logger.info("Collection ${entity.id} was trim into migration task")
            collectionRepository.save(updatedCollection)
        }
        return entity.id.toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TrimCollectionMetaJob::class.java)
    }
}

@Component
class TrimItemMetaJob(
    private val metaTrimmer: MetaTrimmer,
    private val itemRepository: ItemRepository,
) : AbstractTrimJob<ShortItem>() {

    override fun getEntityFrom(from: String?): Flow<ShortItem> {
        return itemRepository.findAll(from?.let { ShortItemId.of(it) })
    }

    override suspend fun trim(item: ShortItem): String {
        val metaEntry = item.metaEntry
        val trimmedData = metaTrimmer.trim(metaEntry?.data)
        if (trimmedData != metaEntry?.data) {
            val updatedCollection = item.copy(metaEntry = metaEntry?.withData(trimmedData))
            logger.info("Item ${item.id} was trim into migration task")
            itemRepository.save(updatedCollection)
        }
        return item.id.toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TrimItemMetaJob::class.java)
    }
}
