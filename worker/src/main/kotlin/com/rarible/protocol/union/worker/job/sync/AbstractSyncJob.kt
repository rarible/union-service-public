package com.rarible.protocol.union.worker.job.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.worker.task.search.EsRateLimiter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory

abstract class AbstractSyncJob<U, E, P : AbstractSyncJobParam>(
    private val entityName: String,
    private val paramClass: Class<P>,
    private val esRateLimiter: EsRateLimiter
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val mapper = ObjectMapper().registerKotlinModule()

    /**
     * Returns next parts of entities to be synced
     */
    abstract suspend fun getNext(param: P, state: String?): Slice<U>

    /**
     * Update data in DB (needed only for entities already stored in Union), without event emission
     */
    abstract suspend fun updateDb(param: P, unionEntities: List<U>): List<E>

    // TODO ideally there should be only Enrichment entities, but it can be done after we complete data migration
    /**
     * Update data in ES, without event emission
     */
    abstract suspend fun updateEs(param: P, enrichmentEntities: List<E>, unionEntities: List<U>)

    /**
     * Send "CHANGE" events into internal topic which means actual data will be delivered to market and ES
     */
    abstract suspend fun notify(param: P, enrichmentEntities: List<E>)

    fun sync(param: String, state: String?): Flow<String> {
        val parsedParam = mapper.readValue(param, paramClass)
        return flow {
            var next = state
            do {
                next = syncBatch(parsedParam, next)
                if (next != null) {
                    emit(next)
                }
            } while (next != null)
        }
    }

    private suspend fun syncBatch(param: P, state: String?): String? {
        logger.info("Fetching {} batch for sync task {}: [{}]", entityName, param, state)
        val slice = getNext(param, state)
        val unionEntities = slice.entities

        if (unionEntities.isEmpty()) {
            logger.info("Sync {} state for {}: there finished with state [{}]", entityName, param, state)
            return null
        }

        val enrichmentEntities = updateDb(param, unionEntities)

        when (param.scope) {
            SyncScope.DB -> {} // Nothing to do, DB should be updated in any case
            SyncScope.ES -> {
                esRateLimiter.waitIfNecessary(enrichmentEntities.size)
                updateEs(param, enrichmentEntities, unionEntities)
            }

            SyncScope.EVENT -> notify(param, enrichmentEntities)
        }

        val newState = slice.continuation
        logger.info(
            "Sync {} state for {}: {} entities updated, new state is [{}]",
            entityName, param, unionEntities.size, newState
        )
        return newState
    }
}