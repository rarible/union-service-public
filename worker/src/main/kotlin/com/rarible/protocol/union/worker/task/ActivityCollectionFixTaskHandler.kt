package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionResolutionRequest
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionResolver
import com.rarible.protocol.union.enrichment.model.EnrichmentActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentActivityId
import com.rarible.protocol.union.enrichment.model.EnrichmentAuctionBidActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentAuctionCancelActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentAuctionEndActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentAuctionFinishActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentAuctionOpenActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentAuctionStartActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentBurnActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentL2DepositActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentL2WithdrawalActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentMintActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderBidActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderCancelBidActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderCancelListActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderListActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderMatchSell
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderMatchSwap
import com.rarible.protocol.union.enrichment.model.EnrichmentTransferActivity
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.worker.job.AbstractBatchJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Deprecated("Remove after execution")
@Component
class ActivityCollectionFixTaskHandler(
    private val job: ActivityCollectionFixTaskJob
) : TaskHandler<String> {

    override val type = "ACTIVITY_COLLECTION_FIX_TASK"

    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}

@Deprecated("Remove after execution")
@Component
class ActivityCollectionFixTaskJob(
    private val repository: ActivityRepository,
    private val customCollectionResolver: CustomCollectionResolver
) : AbstractBatchJob() {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val batchSize = 500

    override suspend fun handleBatch(continuation: String?, param: String): String? {
        val id = continuation?.let { EnrichmentActivityId.of(it) }
        val next = repository.findAll(id, batchSize)
        if (next.isEmpty()) {
            return null
        }
        val withMissingCollections = next.filter { it.collection == null }

        val request = withMissingCollections.mapNotNull { activity ->
            val itemId = activity.itemId
            if (itemId == null) {
                if (activity !is EnrichmentOrderMatchSwap) {
                    logger.warn("Found Activity without Collection and ItemId: {}", activity.id)
                }
                null
            } else {
                CustomCollectionResolutionRequest(activity, IdParser.parseItemId(itemId), null)
            }
        }
        val customCollections = customCollectionResolver.resolve(request, emptyMap())

        coroutineScope {
            withMissingCollections.chunked(24).forEach { chunk ->
                chunk.map {
                    async {
                        val collection = customCollections[it]
                            ?: IdParser.parseCollectionId(it.itemId!!.substringBeforeLast(":"))
                        repository.save(withCollection(it, collection.fullId()))
                    }
                }.awaitAll()
            }
        }
        logger.warn(
            "ACTIVITY_COLLECTION_FIX_TASK state: updated {} of {} collections",
            withMissingCollections.size,
            next.size
        )
        return next.lastOrNull()?.id?.toString()
    }

    private fun withCollection(activity: EnrichmentActivity, collection: String): EnrichmentActivity {
        return when (activity) {
            is EnrichmentMintActivity -> activity.copy(collection = collection)
            is EnrichmentBurnActivity -> activity.copy(collection = collection)
            is EnrichmentTransferActivity -> activity.copy(collection = collection)
            is EnrichmentOrderMatchSwap -> activity.copy(collection = collection)
            is EnrichmentOrderMatchSell -> activity.copy(collection = collection)
            is EnrichmentOrderBidActivity -> activity.copy(collection = collection)
            is EnrichmentOrderListActivity -> activity.copy(collection = collection)
            is EnrichmentOrderCancelBidActivity -> activity.copy(collection = collection)
            is EnrichmentOrderCancelListActivity -> activity.copy(collection = collection)
            is EnrichmentAuctionOpenActivity -> activity.copy(collection = collection)
            is EnrichmentAuctionBidActivity -> activity.copy(collection = collection)
            is EnrichmentAuctionFinishActivity -> activity.copy(collection = collection)
            is EnrichmentAuctionCancelActivity -> activity.copy(collection = collection)
            is EnrichmentAuctionStartActivity -> activity.copy(collection = collection)
            is EnrichmentAuctionEndActivity -> activity.copy(collection = collection)
            is EnrichmentL2DepositActivity -> activity.copy(collection = collection)
            is EnrichmentL2WithdrawalActivity -> activity.copy(collection = collection)
        }
    }
}