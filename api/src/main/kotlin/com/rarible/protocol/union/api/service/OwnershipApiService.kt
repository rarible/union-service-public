package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.AuctionContractService
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentAuctionService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class OwnershipApiService(
    private val orderApiService: OrderApiService,
    private val ownershipRouter: BlockchainRouter<OwnershipService>,
    private val auctionContractService: AuctionContractService,
    private val enrichmentOwnershipService: EnrichmentOwnershipService,
    private val enrichmentAuctionService: EnrichmentAuctionService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getOwnershipById(fullOwnershipId: OwnershipIdDto): OwnershipDto {
        val shortOwnershipId = ShortOwnershipId(fullOwnershipId)

        return coroutineScope {
            val auctionDeferred = async { enrichmentAuctionService.fetchOwnershipAuction(shortOwnershipId) }
            val freeOwnership = enrichmentOwnershipService.fetchOrNull(shortOwnershipId)?.let { enrich(it) }
            val auction = auctionDeferred.await()

            if (freeOwnership != null) {
                // Some or zero of user's items are participated in auction
                enrichmentOwnershipService.mergeWithAuction(freeOwnership, auction)
            } else if (auction != null) {
                // If there is an auction for this item, we need to retrieve its ownership and disguise it
                val auctionOwnershipId = shortOwnershipId.copy(owner = auction.contract.value)
                val auctionUnionOwnership = enrichmentOwnershipService.fetchOrNull(auctionOwnershipId)
                    ?: throw UnionNotFoundException("Auction ownership $auctionOwnershipId not found")

                // we need to enrich ownership BEFORE disguising it
                // such ownership is "fake" and doesn't exist in blockchain DB
                val enriched = enrich(auctionUnionOwnership)

                // All user's items are published in auction, making disguised Ownership here
                enrichmentOwnershipService.disguiseAuctionOwnership(enriched, auction)
            } else {
                throw UnionNotFoundException("Ownership ${fullOwnershipId.fullId()} not found")
            }
        }
    }

    @Deprecated("Should be removed")
    suspend fun enrich(slice: Slice<UnionOwnership>, total: Long): OwnershipsDto? {
        return OwnershipsDto(
            total = total,
            continuation = slice.continuation,
            ownerships = enrich(slice.entities)
        )
    }

    suspend fun enrich(unionOwnershipsPage: Page<UnionOwnership>): OwnershipsDto {
        return OwnershipsDto(
            total = unionOwnershipsPage.total,
            continuation = unionOwnershipsPage.continuation,
            ownerships = enrich(unionOwnershipsPage.entities)
        )
    }

    private suspend fun enrich(unionOwnership: UnionOwnership): OwnershipDto {
        val shortId = ShortOwnershipId(unionOwnership.id)
        val shortOwnership = enrichmentOwnershipService.get(shortId)
        if (shortOwnership == null) {
            return EnrichedOwnershipConverter.convert(unionOwnership)
        }
        return enrichmentOwnershipService.enrichOwnership(shortOwnership, unionOwnership)
    }

    private suspend fun enrich(unionOwnerships: List<UnionOwnership>): List<OwnershipDto> {
        if (unionOwnerships.isEmpty()) {
            return emptyList()
        }

        val existingEnrichedOwnerships: Map<OwnershipIdDto, ShortOwnership> = enrichmentOwnershipService
            .findAll(unionOwnerships.map { ShortOwnershipId(it.id) })
            .associateBy { it.id.toDto() }

        // Looking for full orders for existing ownerships in order-indexer
        val shortOrderIds = existingEnrichedOwnerships.values
            .mapNotNull { it.bestSellOrder?.dtoId }

        val orders = orderApiService.getByIds(shortOrderIds)
            .associateBy { it.id }

        val result = unionOwnerships.map {
            val existingEnrichedOwnership = existingEnrichedOwnerships[it.id]
            if (existingEnrichedOwnership == null) {
                EnrichedOwnershipConverter.convert(it, existingEnrichedOwnership, orders)
            } else {
                enrichmentOwnershipService.enrichOwnership(existingEnrichedOwnership, it, orders)
            }
        }
        return disguiseAuctionOwnerships(result)
    }

    private suspend fun disguiseAuctionOwnerships(ownerships: List<OwnershipDto>): List<OwnershipDto> {
        // TODO won't work in right way with ERC1155
        val auctionOwnerships = coroutineScope {
            ownerships.filter {
                auctionContractService.isAuctionContract(it.blockchain, it.owner.value)
            }.map {
                async {
                    // ATM we expect there could be only ONE auction for item (for ERC721)
                    val ownershipId = it.id
                    val itemId = ItemIdDto(ownershipId.blockchain, ownershipId.contract, ownershipId.tokenId)
                    val auction = enrichmentAuctionService.fetchItemAuction(itemId)

                    if (auction == null) {
                        logger.warn("Auction not found for auction Ownership [{}]", ownershipId)
                        null
                    } else {
                        ownershipId to auction
                    }
                }
            }.awaitAll().filterNotNull().associateBy({ it.first }, { it.second })
        }

        return ownerships.map { ownership ->
            val auction = auctionOwnerships[ownership.id]
            // Replacing auction address by user who initiated the auction
            auction?.let { enrichmentOwnershipService.disguiseAuctionOwnership(ownership, auction) } ?: ownership
        }
    }
}
