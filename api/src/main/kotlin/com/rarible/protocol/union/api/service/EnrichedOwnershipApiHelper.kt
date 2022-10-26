package com.rarible.protocol.union.api.service

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionAuctionOwnershipWrapper
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.core.service.AuctionContractService
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.subchains
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentAuctionService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class EnrichedOwnershipApiHelper(
    private val auctionContractService: AuctionContractService,
    private val enrichmentOwnershipService: EnrichmentOwnershipService,
    private val enrichmentAuctionService: EnrichmentAuctionService,
    private val router: BlockchainRouter<OwnershipService>,
) {

    suspend fun getRawOwnershipsByOwner(owner: UnionAddress, continuation: String?, size: Int): List<UnionOwnership> =
        router.executeForAll(owner.blockchainGroup.subchains()) {
            it.getOwnershipsByOwner(owner.value, continuation, size).entities
        }.flatten()

    suspend fun getRawOwnershipsByItem(itemId: ItemIdDto, continuation: String?, size: Int): List<UnionOwnership> =
        router.getService(itemId.blockchain).getOwnershipsByItem(itemId.value, continuation, size).entities

    suspend fun getOwnershipsByIds(ids: List<OwnershipIdDto>): List<OwnershipDto> {
        val groupedIds = ids.groupBy({ it.blockchain }, { it.value })

        val unionOwnerships = groupedIds.flatMap {
            router.getService(it.key).getOwnershipsByIds(it.value)
        }

        // TODO it works without auction consideration
        return enrich(unionOwnerships.map { UnionAuctionOwnershipWrapper(it, null) })
    }

    suspend fun getOwnershipById(fullOwnershipId: OwnershipIdDto): OwnershipDto {
        val shortOwnershipId = ShortOwnershipId(fullOwnershipId)

        return coroutineScope {
            val auctionDeferred = async { enrichmentAuctionService.fetchOwnershipAuction(shortOwnershipId) }
            val freeOwnership = enrichmentOwnershipService.fetchOrNull(shortOwnershipId)
            val auction = auctionDeferred.await()

            if (freeOwnership != null) {
                // Some or zero of user's items are participated in auction
                enrichmentOwnershipService.mergeWithAuction(enrich(freeOwnership), auction)
            } else if (auction != null) {
                enrichmentOwnershipService.disguiseAuctionWithEnrichment(auction)
                    ?: throw UnionNotFoundException("Ownership ${fullOwnershipId.fullId()} not found")
            } else {
                throw UnionNotFoundException("Ownership ${fullOwnershipId.fullId()} not found")
            }
        }
    }

    suspend fun <T> getEnrichedOwnerships(
        continuation: String?,
        size: Int,
        getAuctions: suspend () -> List<AuctionDto>,
        getOwnerships: suspend (Int) -> List<UnionOwnership>,
        makeResult: suspend (List<UnionAuctionOwnershipWrapper>) -> T,
    ): T {
        val lastPageEnd = DateIdContinuation.parse(continuation)

        val itemAuctions = getAuctions().mapAsync {
            val ownershipId = it.getSellerOwnershipId()
            // Looking for seller's ownerships in order to determine auction is partial
            val ownership = enrichmentOwnershipService.fetchOrNull(ShortOwnershipId(ownershipId))
            UnionAuctionOwnershipWrapper(ownership, it)
        }

        // Looking for full auctions not shown on previous pages
        val fullAuctions = itemAuctions.filter {
            // Works only for DESC ATM
            val fullAuctionContinuation = DateIdContinuation(it.date, it.ownershipId.value)
            val isInPage = lastPageEnd == null || lastPageEnd.compareTo(fullAuctionContinuation) == 1
            it.ownership == null && isInPage
        }.associateBy { it.ownershipId }
        // We don't need to filter partial auctions - related ownerships should be returned in API response,
        // so for them, we can just match these partial auctions
        val partialAuctions = itemAuctions.filter { it.ownership != null }.associateBy { it.ownershipId }

        // We need to query more items than we originally need, because some ownerships can be filtered,
        // if they are belonged to Auction.
        // TODO result could be shorter for case with MAX page size in original request
        val totalSize = PageSize.OWNERSHIP.limit(fullAuctions.size + size)

        val ownerships = getOwnerships(totalSize)
            .filter {
                !auctionContractService.isAuctionContract(it.id.blockchain, it.id.owner.value)
            }

        // Combining partially auctioned and fully auctioned
        val partiallyAuctioned = ownerships.map { UnionAuctionOwnershipWrapper(it, partialAuctions[it.id]?.auction) }
        val fullyAuctioned = fullAuctions.values.map { UnionAuctionOwnershipWrapper(null, it.auction) }
        val combined = partiallyAuctioned + fullyAuctioned

        return makeResult(combined)
    }

    suspend fun enrich(unionOwnerships: List<UnionAuctionOwnershipWrapper>): List<OwnershipDto> {
        return enrichmentOwnershipService.enrich(unionOwnerships)
    }

    suspend fun merge(combined: List<UnionAuctionOwnershipWrapper>): List<UnionOwnership> =
        combined.mapAsync { (ownership, auction) ->
            when {
                ownership != null && auction != null -> enrichmentOwnershipService.mergeWithAuction(ownership, auction)
                auction != null -> enrichmentOwnershipService.disguiseAuction(auction)
                else -> ownership
            }
        }.filterNotNull()

    private suspend fun enrich(unionOwnership: UnionOwnership): OwnershipDto {
        val shortId = ShortOwnershipId(unionOwnership.id)
        val shortOwnership = enrichmentOwnershipService.get(shortId)
            ?: return EnrichedOwnershipConverter.convert(unionOwnership)

        return enrichmentOwnershipService.enrichOwnership(shortOwnership, unionOwnership)
    }
}
