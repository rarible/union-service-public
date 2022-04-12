package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import com.rarible.protocol.union.listener.config.UnionListenerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ReconciliationCollectionJob (
    private val collectionServiceRouter: BlockchainRouter<CollectionService>,
    private val enrichmentRefreshService: EnrichmentRefreshService,
    properties: UnionListenerProperties
) : AbstractReconciliationJob() {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val config = properties.reconciliation

    override suspend fun reconcileBatch(continuation: String?, blockchain: BlockchainDto): String? {
        logger.info("Fetching Collections from {}: [{}]", blockchain.name, continuation)
        val page = collectionServiceRouter.getService(blockchain).getAllCollections(
            continuation,
            config.collectionBatchSize
        )

        val collections = page.entities

        if (collections.isEmpty()) {
            logger.info(
                "RECONCILIATION COLLECTION STATE FOR {}: There is no more Collections for continuation {}",
                blockchain.name, continuation
            )
            return null
        }

        coroutineScope {
            collections.asFlow()
                .map { async { safeUpdate(it.id) } }
                .buffer(config.threadCount)
                .map { it.await() }
                .collect()
        }

        logger.info(
            "RECONCILIATION COLLECTION STATE FOR {}: {} Collections updated, next continuation is [{}]",
            blockchain.name, page.entities.size, page.continuation
        )
        return page.continuation
    }

    private suspend fun safeUpdate(collectionId: CollectionIdDto) {
        try {
            enrichmentRefreshService.reconcileCollection(collectionId)
        } catch (e: Exception) {
            logger.error("Unable to reconcile collection {} : {}", e.message, e)
        }
    }
}