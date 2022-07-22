package com.rarible.protocol.union.enrichment.service.query.ownership

import com.rarible.protocol.union.enrichment.service.query.order.OrderApiMergeService
import com.rarible.protocol.union.core.continuation.UnionAuctionOwnershipWrapperContinuation
import com.rarible.protocol.union.core.continuation.UnionOwnershipContinuation
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionAuctionOwnershipWrapper
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.core.service.AuctionContractService
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.subchains
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentAuctionService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class OwnershipApiService(
    private val orderApiService: OrderApiMergeService,
    private val ownershipRouter: BlockchainRouter<OwnershipService>,
    private val auctionContractService: AuctionContractService,
    private val enrichmentOwnershipService: EnrichmentOwnershipService,
    private val enrichmentAuctionService: EnrichmentAuctionService
) {
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
                val resultOwnership = enrichmentOwnershipService.disguiseAuctionWithEnrichment(auction)
                    ?: throw UnionNotFoundException("Ownership ${fullOwnershipId.fullId()} not found")

                resultOwnership
            } else {
                throw UnionNotFoundException("Ownership ${fullOwnershipId.fullId()} not found")
            }
        }
    }

    suspend fun getOwnershipByOwner(owner: UnionAddress, continuation: String?, size: Int): Slice<UnionOwnership> {
        val lastPageEnd = DateIdContinuation.parse(continuation)
        val ownerships = ownershipRouter.executeForAll(owner.blockchainGroup.subchains()) {
            it.getOwnershipsByOwner(owner.value, continuation, size)
        }.flatMap { it.entities }

        // owner auctions
        val ownerAuctions = coroutineScope {
            enrichmentAuctionService.findBySeller(owner).map {
                async {
                    val ownershipId = it.getSellerOwnershipId()
                    val ownership = enrichmentOwnershipService.fetchOrNull(ShortOwnershipId(ownershipId))
                    UnionAuctionOwnershipWrapper(ownership, it)
                }
            }.awaitAll()
        }

        // Looking for full auctions not shown on previous pages
        val fullAuctions = ownerAuctions.filter {
            // Works only for DESC ATM
            val fullAuctionContinuation = DateIdContinuation(it.date, it.ownershipId.value)
            val isInPage = lastPageEnd == null || lastPageEnd.compareTo(fullAuctionContinuation) == 1
            it.ownership == null && isInPage
        }.associateBy { it.ownershipId }

        // We don't need to filter partial auctions - related ownerships should be returned in API response,
        // so for them, we can just match these partial auctions
        val partialAuctions = ownerAuctions.filter { it.ownership != null }.associateBy { it.ownershipId }

        // Combining partially auctioned and fully auctioned
        val partiallyAuctioned = ownerships.map { UnionAuctionOwnershipWrapper(it, partialAuctions[it.id]?.auction) }
        val fullyAuctioned = fullAuctions.values.map { UnionAuctionOwnershipWrapper(null, it.auction) }

        // Now we can trim combined list to requested size
        val page = Paging(
            UnionOwnershipContinuation.ByLastUpdatedAndId,
            merge(partiallyAuctioned + fullyAuctioned)
        ).getSlice(size)

        return page
    }

    suspend fun getOwnershipsByItem(itemId: ItemIdDto, continuation: String?, size: Int): OwnershipsDto {
        val shortItemId = ShortItemId(itemId)
        val lastPageEnd = DateIdContinuation.parse(continuation)

        val itemAuctions = coroutineScope {
            enrichmentAuctionService.findByItem(shortItemId).map {
                async {
                    val ownershipId = it.getSellerOwnershipId()
                    // Looking for seller's ownerships in order to determine auction is partial
                    val ownership = enrichmentOwnershipService.fetchOrNull(ShortOwnershipId(ownershipId))
                    UnionAuctionOwnershipWrapper(ownership, it)
                }
            }.awaitAll()
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

        val ownershipPage = ownershipRouter.getService(itemId.blockchain)
            .getOwnershipsByItem(itemId.value, continuation, totalSize)

        val ownerships = ownershipPage.entities.filter {
            // Removing all auction ownerships
            !auctionContractService.isAuctionContract(it.id.blockchain, it.id.owner.value)
        }

        // Combining partially auctioned and fully auctioned
        val partiallyAuctioned = ownerships.map { UnionAuctionOwnershipWrapper(it, partialAuctions[it.id]?.auction) }
        val fullyAuctioned = fullAuctions.values.map { UnionAuctionOwnershipWrapper(null, it.auction) }
        val combined = partiallyAuctioned + fullyAuctioned

        // Now we can trim combined list to requested size
        val page = Paging(UnionAuctionOwnershipWrapperContinuation.ByLastUpdatedAndId, combined)
            .getSlice(size)

        val result = enrich(page.entities)

        return OwnershipsDto(ownershipPage.total, page.continuation, result)
    }

    private suspend fun enrich(unionOwnerships: List<UnionAuctionOwnershipWrapper>): List<OwnershipDto> {
        if (unionOwnerships.isEmpty()) {
            return emptyList()
        }

        val existingEnrichedOwnerships: Map<OwnershipIdDto, ShortOwnership> = enrichmentOwnershipService
            .findAll(unionOwnerships.mapNotNull { it.ownership?.let { ShortOwnershipId(it.id) } })
            .associateBy { it.id.toDto() }

        // Looking for full orders for existing ownerships in order-indexer
        val shortOrderIds = existingEnrichedOwnerships.values
            .map { it.getAllBestOrders() }
            .flatten()
            .map { it.dtoId }

        val orders = orderApiService.getByIds(shortOrderIds)
            .associateBy { it.id }

        val result = coroutineScope {
            unionOwnerships.map {
                async {
                    if (it.ownership != null) {
                        // If there is an ownership, we use it as primary entity
                        val existingEnrichedOwnership = existingEnrichedOwnerships[it.ownershipId]
                        // Enriching it if possible
                        val ownership = if (existingEnrichedOwnership == null) {
                            EnrichedOwnershipConverter.convert(it.ownership!!)
                        } else {
                            enrichmentOwnershipService.enrichOwnership(existingEnrichedOwnership, it.ownership, orders)
                        }
                        // Merge with related auction if possible
                        enrichmentOwnershipService.mergeWithAuction(ownership, it.auction)
                    } else {
                        // If we have fully auctioned ownership, it should be disguised as Ownership
                        enrichmentOwnershipService.disguiseAuctionWithEnrichment(it.auction!!)
                    }
                }
            }.awaitAll().filterNotNull()
        }
        return result
    }

    private suspend fun enrich(unionOwnership: UnionOwnership): OwnershipDto {
        val shortId = ShortOwnershipId(unionOwnership.id)
        val shortOwnership = enrichmentOwnershipService.get(shortId)
            ?: return EnrichedOwnershipConverter.convert(unionOwnership)

        return enrichmentOwnershipService.enrichOwnership(shortOwnership, unionOwnership)
    }

    private suspend fun merge(combined: List<UnionAuctionOwnershipWrapper>): List<UnionOwnership> = coroutineScope {
        combined.map {
            async {
                val (ownership, auction) = it.split()
                when {
                    ownership != null && auction != null -> enrichmentOwnershipService.mergeWithAuction(ownership, auction)
                    auction != null -> enrichmentOwnershipService.disguiseAuction(auction)
                    else -> it.ownership
                }
            }
        }.awaitAll().filterNotNull()
    }
}

fun UnionAuctionOwnershipWrapper.split(): Pair<UnionOwnership?, AuctionDto?> = Pair(this.ownership, this.auction)
