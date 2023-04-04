package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaService
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SyncCollectionJob(
    private val collectionServiceRouter: BlockchainRouter<CollectionService>,
    private val enrichmentCollectionService: EnrichmentCollectionService,
    private val collectionMetaService: CollectionMetaService
) : AbstractReconciliationJob() {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val batchSize = 50

    override suspend fun reconcileBatch(continuation: String?, blockchain: BlockchainDto): String? {
        logger.info("Fetching Collections from {}: [{}]", blockchain.name, continuation)
        val page = collectionServiceRouter.getService(blockchain).getAllCollections(
            continuation,
            batchSize
        )

        val collections = page.entities

        if (collections.isEmpty()) {
            logger.info(
                "SYNC COLLECTION STATE FOR {}: There is no more Collections for continuation {}",
                blockchain, continuation
            )
            return null
        }

        coroutineScope {
            collections
                .map { async { sync(it) } }
                .awaitAll()
        }

        logger.info(
            "SYNC COLLECTION STATE FOR {}: {} Collections updated, next continuation is [{}]",
            blockchain.name, page.entities.size, page.continuation
        )
        return page.continuation
    }

    private suspend fun sync(collection: UnionCollection) {
        try {
            // There is no need to send updates, we do NOT change actual data
            val enrichmentCollection = enrichmentCollectionService.update(collection, false)
            if (enrichmentCollection.metaEntry == null) {
                collectionMetaService.schedule(collection.id, CollectionMetaPipeline.SYNC, false)
            }
        } catch (e: Exception) {
            logger.error("Failed to sync collection {} : {}", collection.id, e.message, e)
        }
    }
}