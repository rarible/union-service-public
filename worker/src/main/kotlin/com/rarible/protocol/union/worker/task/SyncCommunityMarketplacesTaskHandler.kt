package com.rarible.protocol.union.worker.task

import com.rarible.core.logging.withTraceId
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.worker.cmp.client.CommunityMarketplaceClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Component
class SyncCommunityMarketplacesTaskHandler(
    private val communityMarketplaceClient: CommunityMarketplaceClient,
    private val collectionRepository: CollectionRepository,
) : TaskHandler<String> {
    override val type: String
        get() = "SYNC_COMMUNITY_MARKETPLACES_TASK"

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        logger.info("Starting SyncCommunityMarketplacesTaskHandler")
        val lastId = AtomicReference(from)
        val marketplacesSyncedCount = AtomicInteger()

        while (true) {
            val marketplaces = communityMarketplaceClient.getMarketplaces(lastId.get())
            if (marketplaces.isEmpty()) {
                break
            }
            marketplaces.forEach {
                marketplacesSyncedCount.incrementAndGet()
                collectionRepository.updatePriority(
                    collectionIds = it.collectionIds,
                    priority = it.metaRefreshPriority,
                )
            }
            val lastProcessedId = marketplaces.last().id
            logger.info("Processed ${marketplaces.size} marketplaces. Last id: $lastProcessedId")
            lastId.set(lastProcessedId)
            emit(lastProcessedId)
        }
        logger.info(
            "Finished SyncCommunityMarketplacesTaskHandler. Synced ${marketplacesSyncedCount.get()} marketplaces"
        )
    }.withTraceId()

    companion object {
        private val logger = LoggerFactory.getLogger(SyncCommunityMarketplacesTaskHandler::class.java)
    }
}
