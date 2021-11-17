package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionIdDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentAuctionService(
    private val auctionServiceRouter: BlockchainRouter<AuctionService>
) {

    private val logger = LoggerFactory.getLogger(EnrichmentAuctionService::class.java)

    suspend fun fetchAuctionsIfAbsent(
        ids: Set<AuctionIdDto>,
        auctions: Map<AuctionIdDto, AuctionDto>
    ): List<AuctionDto> {
        val requestedIds = ids - auctions.keys

        val fetched = requestedIds.isNotEmpty().let {
            requestedIds.groupBy { it.blockchain }.flatMap { (k, v) ->
                logger.info("Fetching {} auctions with ids: {}", k, v)
                val data = auctionServiceRouter.getService(k).getAuctionsByIds(v.map { it.value })
                logger.info("Fetched {} auctions", data.size)
                data
            }
        }

        return auctions.values.toList() + fetched
    }

}
