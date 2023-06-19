package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.producer.UnionInternalItemEventProducer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BestBidCleanupJob(
    private val itemRepository: ItemRepository,
    private val producer: UnionInternalItemEventProducer
) : AbstractReconciliationJob() {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val batchSize = 200

    override suspend fun reconcileBatch(continuation: String?, blockchain: BlockchainDto): String? {
        val itemId = continuation?.let { IdParser.parseItemId(it) }?.let { ShortItemId(it) }
        val items = itemRepository.findByBlockchain(itemId, blockchain, batchSize).toList()

        if (items.isEmpty()) {
            logger.info(
                "BEST BID CLEANUP STATE FOR {}: There is no more Items for continuation {}",
                blockchain.name, continuation
            )
            return null
        }

        val withBids = items.filter { it.bestBidOrder != null }
        withBids.forEach {
            itemRepository.save(it.copy(bestBidOrder = null, bestBidOrders = emptyMap()))
            producer.sendChangeEvent(it.id.toDto())
        }

        logger.info("Best bids cleaned up for {}, {}/{} updated", blockchain, items.size, withBids.size)
        return items.lastOrNull()?.id?.toDto()?.fullId()
    }
}