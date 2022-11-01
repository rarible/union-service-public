package com.rarible.protocol.union.enrichment.meta.item.migration

import com.rarible.core.loader.internal.common.LoadTask
import com.rarible.core.loader.internal.common.LoadTaskUpdateListener
import com.rarible.loader.cache.internal.CacheRepository
import com.rarible.loader.cache.internal.MongoCacheEntry
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@Deprecated("Should be removed after meta-pipeline migration")
class ItemMetaMigrator(
    private val legacyRepository: CacheRepository,
    private val modernRepository: ItemMetaRepository
) : LoadTaskUpdateListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onTaskSaved(task: LoadTask) {
        migrate(task)
    }

    /**
     * Single migration of the task, not applicable for batches. Should be used for realtime migrations
     */
    suspend fun migrate(task: LoadTask) {
        val migration = when (task.status) {
            is LoadTask.Status.Loaded -> {
                val legacy = legacyRepository.get<UnionMeta>(ItemMetaDownloader.TYPE, task.key)
                val modern = modernRepository.get(task.key)
                getDownloadedMigration(task, modern, legacy)
            }
            is LoadTask.Status.Failed -> {
                val modern = modernRepository.get(task.key)
                getFailedMigration(task, modern)
            }
            else -> null // Other statuses are not needed
        }
        migration?.let { migrate(migration) }
    }

    /**
     * Batch migration, should be used in background job
     */
    suspend fun migrate(tasks: List<LoadTask>, asyncTaskCount: Int) = coroutineScope {
        val filtered = tasks.filter { it.status is LoadTask.Status.Loaded || it.status is LoadTask.Status.Failed }
        val groupedByItem = filtered.groupBy { it.key }

        // We need legacy meta only for successful tasks
        val downloadedKeys = filtered.filter { it.status is LoadTask.Status.Loaded }.map { it.key }
        // But for modern meta we have to retrieve all entries in order to execute safe update
        val allKeys = groupedByItem.keys.toList()

        val modernMetaDeferred = async { modernRepository.getAll(allKeys).associateBy { it.id } }
        val legacyMeta = legacyRepository.getAll<UnionMeta>(ItemMetaDownloader.TYPE, downloadedKeys)
            .associateBy { it.key }
        val modernMeta = modernMetaDeferred.await()

        filtered.mapNotNull { task ->
            val modern = modernMeta[task.key]
            val legacy = legacyMeta[task.key]
            when (task.status) {
                is LoadTask.Status.Loaded -> getDownloadedMigration(task, modern, legacy)
                is LoadTask.Status.Failed -> getFailedMigration(task, modern)
                else -> null
            }
        }.filter { it.isMigrationRequired() }
            .chunked(asyncTaskCount)
            .map { chunk -> chunk.map { migration -> async { migrate(migration) } }.awaitAll() }
    }

    private fun getDownloadedMigration(
        task: LoadTask,
        modern: DownloadEntry<UnionMeta>?,
        legacy: MongoCacheEntry<UnionMeta>?
    ): DownloadedMetaMigration? {
        if (legacy == null) {
            logger.info("Meta with key [{}] not found not found for Loaded task [{}]", task.key, task.id)
            return null
        }
        return DownloadedMetaMigration(task, modern, legacy)
    }

    private fun getFailedMigration(
        task: LoadTask,
        modern: DownloadEntry<UnionMeta>?
    ): FailedMetaMigration {
        return FailedMetaMigration(task, modern)
    }

    suspend fun migrate(migration: ItemMetaMigration) {
        modernRepository.update(migration.task.key, migration::isMigrationRequired, migration::update)
    }
}

