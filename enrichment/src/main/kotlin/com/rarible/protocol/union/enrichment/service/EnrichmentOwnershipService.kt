package com.rarible.protocol.union.enrichment.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ItemSellStats
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException

@Component
@CaptureSpan(type = SpanType.APP)
class EnrichmentOwnershipService(
    private val ownershipServiceRouter: BlockchainRouter<OwnershipService>,
    private val ownershipRepository: OwnershipRepository,
    private val enrichmentOrderService: EnrichmentOrderService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentOwnershipService::class.java)

    suspend fun get(ownershipId: ShortOwnershipId): ShortOwnership? {
        return ownershipRepository.get(ownershipId)
    }

    suspend fun getOrEmpty(ownershipId: ShortOwnershipId): ShortOwnership {
        return ownershipRepository.get(ownershipId) ?: ShortOwnership.empty(ownershipId)
    }

    suspend fun save(ownership: ShortOwnership): ShortOwnership {
        return ownershipRepository.save(ownership.withCalculatedFields())
    }

    suspend fun delete(ownershipId: ShortOwnershipId): DeleteResult? {
        val result = ownershipRepository.delete(ownershipId)
        logger.debug("Deleted Ownership [{}], deleted: {}", ownershipId, result?.deletedCount)
        return result
    }

    suspend fun findAll(ids: List<ShortOwnershipId>): List<ShortOwnership> {
        return ownershipRepository.getAll(ids)
    }

    suspend fun getItemSellStats(itemId: ShortItemId): ItemSellStats {
        val now = nowMillis()
        val result = ownershipRepository.getItemSellStats(itemId)
        logger.info("SellStat query executed for ItemId [{}]: [{}] ({}ms)", itemId, result, spent(now))
        return result
    }

    suspend fun fetch(ownershipId: ShortOwnershipId): UnionOwnership {
        return ownershipServiceRouter.getService(ownershipId.blockchain)
            .getOwnershipById(ownershipId.toDto().value)
    }

    suspend fun fetchOrNull(ownershipId: ShortOwnershipId): UnionOwnership? {
        return try {
            fetch(ownershipId)
        } catch (e: WebClientResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                null
            } else {
                throw e
            }
        }
    }

    suspend fun fetchAllByItemId(itemId: ShortItemId): List<UnionOwnership> {
        var continuation: String? = null
        val result = ArrayList<UnionOwnership>()
        do {
            val page = ownershipServiceRouter.getService(itemId.blockchain).getOwnershipsByItem(
                itemId.toDto().value,
                continuation,
                PageSize.OWNERSHIP.max
            )
            result.addAll(page.entities)
            continuation = page.continuation
        } while (continuation != null)
        return result
    }

    suspend fun enrichOwnership(
        short: ShortOwnership,
        ownership: UnionOwnership? = null,
        orders: Map<OrderIdDto, OrderDto> = emptyMap()
    ) = coroutineScope {
        val fetchedOwnership = async { ownership ?: fetch(short.id) }
        val bestSellOrder = enrichmentOrderService.fetchOrderIfDiffers(short.bestSellOrder, orders)

        val bestOrders = listOfNotNull(bestSellOrder)
            .associateBy { it.id }

        EnrichedOwnershipConverter.convert(fetchedOwnership.await(), short, bestOrders)
    }

    fun mergeWithAuction(ownership: OwnershipDto, auction: AuctionDto?): OwnershipDto {
        return if (auction != null) {
            // Some part of user's items are participated in auction, combining total value (ER1155 case)
            ownership.copy(
                value = ownership.value + auction.sell.value.toBigInteger(),
                auction = auction
            )
        } else {
            // There is no auction created by user for this item, return ownership "as is"
            ownership
        }
    }

    fun mergeWithAuction(ownership: UnionOwnership, auction: AuctionDto): UnionOwnership =
        ownership.copy(value = ownership.value + auction.sell.value.toBigInteger())

    suspend fun disguiseAuctionWithEnrichment(auction: AuctionDto): OwnershipDto? {
        return withDisguising(auction) {
            // we need to enrich ownership BEFORE disguising it
            // such ownership is "fake" and doesn't exist in blockchain DB
            disguiseAuctionOwnership(EnrichedOwnershipConverter.convert(it), auction)
        }
    }

    suspend fun disguiseAuction(auction: AuctionDto): UnionOwnership? {
        return withDisguising(auction) {
            // without enrichment
            disguiseAuctionOwnership(it, auction)
        }
    }

    private suspend fun <T> withDisguising(auction: AuctionDto, call: (ownership: UnionOwnership) -> T): T? {
        // If there is an auction for this item, we need to retrieve its ownership and disguise it
        val sellerOwnershipId = ShortOwnershipId(auction.getSellerOwnershipId())
        val auctionOwnershipId = sellerOwnershipId.copy(owner = auction.contract.value)
        val auctionUnionOwnership = fetchOrNull(auctionOwnershipId)

        return if (auctionUnionOwnership != null) {

            // All user's items are published in auction, making disguised Ownership here
            call(auctionUnionOwnership)
        } else {
            // Originally, it should not happen, but for page responses it is better to skip
            // entity instead of completely fail the entire request
            logger.warn("Auction ownership [{}] not found", auctionOwnershipId)
            null
        }
    }

    private fun disguiseAuctionOwnership(auctionOwnership: OwnershipDto, auction: AuctionDto): OwnershipDto {
        val id = auctionOwnership.id
        return auctionOwnership.copy(
            id = OwnershipIdDto(id.blockchain, id.itemIdValue, auction.seller),
            owner = auction.seller,
            auction = auction,
            // Auction ownership may have summarized value from several auctions, so here we need to use value
            // from 'sell' field from auction entity
            value = auction.sell.value.toBigInteger(),
            createdAt = auction.createdAt
        )
    }

    private fun disguiseAuctionOwnership(auctionOwnership: UnionOwnership, auction: AuctionDto): UnionOwnership {
        val id = auctionOwnership.id
        return auctionOwnership.copy(
            id = OwnershipIdDto(id.blockchain, id.itemIdValue, auction.seller),
            value = auction.sell.value.toBigInteger(),
            createdAt = auction.createdAt
        )
    }
}
