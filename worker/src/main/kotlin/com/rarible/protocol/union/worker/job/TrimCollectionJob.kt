package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.enrichment.meta.item.MetaTrimmer
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component

abstract class AbstractTrimJob<Entity> {

    fun trim(from: String?) = getEntityFrom(from).map { entity ->
        trimWithOptimisticLock(entity)
    }

    private suspend fun trimWithOptimisticLock(entity: Entity): String {
        val attempts: Long = 5
        var retry = 0
        var last: Throwable

        var latestEntityVersion: Entity = entity
        do {
            last = try {
                return trim(latestEntityVersion)
            } catch (ex: OptimisticLockingFailureException) {
                ex
            } catch (ex: DuplicateKeyException) {
                ex
            }
            val (entityId, latestEntity) = getLatestEntityVersion(entity)
            if (latestEntity == null) return entityId

            latestEntityVersion = latestEntity
        } while (++retry < attempts)

        throw last
    }

    protected abstract fun getEntityFrom(from: String?): Flow<Entity>

    protected abstract suspend fun getLatestEntityVersion(entity: Entity): Pair<String, Entity?>

    protected abstract suspend fun trim(entity: Entity): String
}

@Component
class TrimCollectionMetaJob(
    private val metaTrimmer: MetaTrimmer,
    private val collectionRepository: CollectionRepository,
) : AbstractTrimJob<EnrichmentCollection>() {

    override fun getEntityFrom(from: String?): Flow<EnrichmentCollection> {
        return collectionRepository.findAll(from?.let { EnrichmentCollectionId.of(it) })
    }

    override suspend fun getLatestEntityVersion(entity: EnrichmentCollection): Pair<String, EnrichmentCollection?> {
        return entity.id.toString() to collectionRepository.get(entity.id)
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

    override suspend fun getLatestEntityVersion(entity: ShortItem): Pair<String, ShortItem?> {
        return entity.id.toString() to itemRepository.get(entity.id)
    }

    override suspend fun trim(entity: ShortItem): String {
        val metaEntry = entity.metaEntry
        val trimmedData = metaTrimmer.trim(metaEntry?.data)
        if (trimmedData != metaEntry?.data) {
            val updatedItem = entity.copy(metaEntry = metaEntry?.withData(trimmedData))
            logger.info("Item ${entity.id} was trim into migration task")
            itemRepository.save(updatedItem)
        }
        return entity.id.toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TrimItemMetaJob::class.java)
    }
}
