package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.listener.config.UnionListenerProperties
import com.rarible.protocol.union.listener.service.EnrichmentAuctionEventService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ReconciliationAuctionsJob(
    private val auctionServiceRouter: BlockchainRouter<AuctionService>,
    private val auctionEventService: EnrichmentAuctionEventService,
    properties: UnionListenerProperties
) : AbstractReconciliationJob() {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val config = properties.reconciliation

    override suspend fun reconcileBatch(continuation: String?, blockchain: BlockchainDto): String? {
        logger.info("Fetching auctions from {}: [{}]", blockchain.name, continuation)
        val page = auctionServiceRouter.getService(blockchain).getAuctionsAll(
            platform = null,
            continuation = continuation,
            size = config.auctionBatchSize
        )

        val auctions = page.entities

        if (auctions.isEmpty()) {
            logger.info(
                "RECONCILIATION STATE FOR {}: There is no more auctions for continuation {}, aborting reconciliation",
                blockchain.name, continuation
            )
            return null
        }

        coroutineScope {
            auctions.asFlow().map { async { safeUpdate(it) } }
                .buffer(config.threadCount)
                .map { it.await() }
                .collect()
        }

        logger.info(
            "RECONCILIATION STATE FOR {}: {} auctions updated, next continuation is [{}]",
            blockchain.name, page.entities.size, page.continuation
        )
        return page.continuation
    }

    private suspend fun safeUpdate(auction: AuctionDto) {
        try {
            auctionEventService.onAuctionUpdated(auction)
        } catch (e: Exception) {
            logger.error("Unable to reconcile auction {} : {}", e.message, e)
        }
    }
}
