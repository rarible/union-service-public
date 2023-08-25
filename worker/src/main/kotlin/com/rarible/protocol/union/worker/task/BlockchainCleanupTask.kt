package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.model.EnrichmentActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.repository.search.ElasticSearchRepository
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.worker.job.AbstractBlockchainBatchJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class BlockchainCleanupTask(
    private val job: BlockchainCleanupJob
) : TaskHandler<String> {

    override val type = "BLOCKCHAIN_CLEANUP_TASK"

    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}

@Component
class BlockchainCleanupJob(
    private val template: ReactiveMongoTemplate,
    private val esCollectionRepository: EsCollectionRepository,
    private val esItemRepository: EsItemRepository,
    private val esOwnershipRepository: EsOwnershipRepository,
    private val esActivityRepository: EsActivityRepository,
) : AbstractBlockchainBatchJob() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleBatch(continuation: String?, blockchain: BlockchainDto): String? {
        coroutineScope {
            listOf(
                async { delete(blockchain, EnrichmentCollection.COLLECTION, esCollectionRepository) },
                async { delete(blockchain, ShortItem.COLLECTION, esItemRepository) },
                async { delete(blockchain, ShortOwnership.COLLECTION, esOwnershipRepository) },
                async { delete(blockchain, EnrichmentActivity.COLLECTION, esActivityRepository) },
            ).awaitAll()
        }
        return null
    }

    private suspend fun delete(
        blockchain: BlockchainDto,
        mongoCollection: String,
        esRepository: ElasticSearchRepository<*>
    ) = coroutineScope {
        try {
            logger.info("Deleting $blockchain entities from $mongoCollection")

            val mongoQuery = Query(Criteria("_id.blockchain").isEqualTo(blockchain))
            val mongo = async { template.remove(mongoQuery, mongoCollection).awaitSingle() }
            val esResult = esRepository.deleteByBlockchain(blockchain)
            val mongoResult = mongo.await()

            logger.info(
                "Deleted ${mongoResult.deletedCount} $blockchain entities from $mongoCollection " +
                    "and ${esResult.deleted} records from ES"
            )
        } catch (e: Exception) {
            logger.info(
                "Deletion of $blockchain entities from " +
                    "$mongoCollection takes a lot of time, stop waiting for the result", e
            )
        }
    }
}
