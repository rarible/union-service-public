package com.rarible.protocol.union.enrichment.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.subchains
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.APP)
class EnrichmentAuctionService(
    private val auctionServiceRouter: BlockchainRouter<AuctionService>
) {

    private val logger = LoggerFactory.getLogger(EnrichmentAuctionService::class.java)
    private val FETCH_SIZE = 1_000

    suspend fun fetchOwnershipAuction(shortOwnershipId: ShortOwnershipId): AuctionDto? {
        val auctionPage = auctionServiceRouter.getService(shortOwnershipId.blockchain)
            .getAuctionsByItem(
                itemId = shortOwnershipId.toDto().itemIdValue,
                seller = shortOwnershipId.owner,
                status = listOf(AuctionStatusDto.ACTIVE),
                size = 1
            )
        return auctionPage.entities.firstOrNull()
    }

    suspend fun fetchAuctionsIfAbsent(
        ids: Set<AuctionIdDto>,
        auctions: Map<AuctionIdDto, AuctionDto>
    ): Map<AuctionIdDto, AuctionDto> {
        val requestedIds = ids - auctions.keys

        val fetched = requestedIds.isNotEmpty().let {
            requestedIds.groupBy { it.blockchain }.flatMap { (k, v) ->
                val data = auctionServiceRouter.getService(k).getAuctionsByIds(v.map { it.value })
                logger.info("Fetched {} auctions by ids", data.size, requestedIds)
                data
            }
        }

        return auctions + fetched.associateBy { it.id }
    }

    suspend fun findByItem(itemId: ShortItemId): List<AuctionDto> {
        logger.info("Fetching all auction for item {}", itemId)

        var continuation: String? = null
        val result = ArrayList<AuctionDto>()

        do {
            val page = auctionServiceRouter.getService(itemId.blockchain).getAuctionsByItem(
                itemId.toDto().value,
                null,
                null,
                null,
                listOf(AuctionStatusDto.ACTIVE),
                null,
                null,
                continuation,
                FETCH_SIZE
            )
            result.addAll(page.entities)
            continuation = page.continuation
        } while (continuation != null)

        logger.info("Fetched {} auctions for item [{}]", result.size, itemId)
        return result
    }

    suspend fun findBySeller(seller: UnionAddress): List<AuctionDto> {
        logger.info("Fetching all auction for seller {}", seller)

        var continuation: String? = null
        val result = ArrayList<AuctionDto>()

        val blockchains = seller.blockchainGroup.subchains()
        blockchains.map { blockchain ->
            coroutineScope {
                async {
                    do {
                        val page = auctionServiceRouter.getService(blockchain).getAuctionsBySeller(
                            seller.value, listOf(AuctionStatusDto.ACTIVE), null, null, continuation, FETCH_SIZE)
                        result.addAll(page.entities)
                        continuation = page.continuation
                    } while (continuation != null)
                }
            }
        }.awaitAll()

        logger.info("Fetched {} auctions for seller [{}]", result.size, seller)
        return result
    }

}
