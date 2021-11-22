package com.rarible.protocol.union.enrichment.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.APP)
class EnrichmentAuctionService(
    private val auctionServiceRouter: BlockchainRouter<AuctionService>
) {

    private val logger = LoggerFactory.getLogger(EnrichmentAuctionService::class.java)
    private val FETCH_SIZE = 1_000

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

    fun findByItem(item: ShortItem): Flow<AuctionDto> = flow {
        var continuation: String? = null
        logger.info("Fetching all auction for item {}", item.id)
        var count = 0
        do {
            val page = auctionServiceRouter.getService(item.blockchain).getAuctionsByItem(
                item.id.token,
                item.id.tokenId.toString(),
                null,
                null,
                null,
                listOf(AuctionStatusDto.ACTIVE),
                null,
                null,
                continuation,
                FETCH_SIZE
            )
            page.entities.forEach { emit(it) }
            count += page.entities.count()
            continuation = page.continuation
        } while (continuation != null)
        logger.info("Fetched {} auctions for item {}", count, item.id)
    }

}
