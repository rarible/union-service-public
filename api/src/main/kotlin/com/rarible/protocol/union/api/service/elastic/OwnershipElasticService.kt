package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.service.EnrichedOwnershipApiHelper
import com.rarible.protocol.union.api.service.OwnershipQueryService
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipSearchRequestDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.subchains
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentAuctionService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipElasticService(
    val enrichmentAuctionService: EnrichmentAuctionService,
    private val apiHelper: EnrichedOwnershipApiHelper,
    private val elasticHelper: OwnershipElasticHelper,
    private val router: BlockchainRouter<OwnershipService>
) : OwnershipQueryService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getOwnershipById(fullOwnershipId: OwnershipIdDto): OwnershipDto =
        apiHelper.getOwnershipById(fullOwnershipId)

    override suspend fun getOwnershipsByIds(ids: List<OwnershipIdDto>): List<OwnershipDto> {
        return apiHelper.getOwnershipsByIds(ids)
    }

    override suspend fun getOwnershipByOwner(
        owner: UnionAddress,
        continuation: String?,
        size: Int,
    ): Slice<UnionOwnership> {
        val enabledBlockchains = router.getEnabledBlockchains(owner.blockchainGroup.subchains()).toSet()
        if (enabledBlockchains.isEmpty()) {
            logger.info("Unable to find enabled blockchains for getOwnershipByOwner(), owner={}", owner)
            return Slice.empty()
        }
        val (cursor, entities) = elasticHelper.getRawOwnershipsByOwner(owner, enabledBlockchains, continuation, size)
        return apiHelper.getEnrichedOwnerships(
            continuation,
            size,
            { enrichmentAuctionService.findBySeller(owner) },
            { entities },
            { ownerships ->
                Slice(
                    continuation = cursor,
                    entities = apiHelper.merge(ownerships),
                )
            }
        )
    }

    suspend fun getOwnershipsByCollection(collectionId: CollectionIdDto, continuation: String?, size: Int): OwnershipsDto {
        if (!router.isBlockchainEnabled(collectionId.blockchain)) {
            logger.info("Unable to find enabled blockchains for getOwnershipsByCollection(), collectionId={}", collectionId)
            return OwnershipsDto()
        }
        val (cursor, entities) = elasticHelper.getRawOwnershipsByCollection(collectionId, continuation, size)
        return apiHelper.getEnrichedOwnerships(
            continuation,
            size,
            // TODO auctions are disabled
            { emptyList() },
            { entities },
            { ownerships ->
                OwnershipsDto(
                    null,
                    cursor,
                    apiHelper.enrich(ownerships))
            }
        )
    }

    override suspend fun getOwnershipsByItem(
        itemId: ItemIdDto,
        continuation: String?,
        size: Int,
    ): OwnershipsDto {
        if (!router.isBlockchainEnabled(itemId.blockchain)) {
            logger.info("Unable to find enabled blockchains for getOwnershipsByItem(), item={}", itemId)
            return OwnershipsDto()
        }
        val (cursor, entities) = elasticHelper.getRawOwnershipsByItem(itemId, continuation, size)
        return apiHelper.getEnrichedOwnerships(
            continuation,
            size,
            { enrichmentAuctionService.findByItem(ShortItemId(itemId)) },
            { entities },
            { ownerships ->
                OwnershipsDto(
                    null,
                    cursor,
                    apiHelper.enrich(ownerships))
            }
        )
    }

    override suspend fun search(request: OwnershipSearchRequestDto): OwnershipsDto {
        val blockchains = request.filter.blockchains
        if (blockchains != null && blockchains.isNotEmpty()) {
            if (blockchains.none { router.isBlockchainEnabled(it) }) {
                logger.info("Unable to find enabled blockchains for Ownership's search(), request={}", request)
                return OwnershipsDto()
            }
        }
        val auctions = coroutineScope {
            listOf(
                async {
                    request.filter.items?.flatMap {
                        enrichmentAuctionService.findByItem(ShortItemId(it))
                    }.orEmpty()
                },
                async {
                    request.filter.owners?.flatMap {
                        enrichmentAuctionService.findBySeller(it)
                    }.orEmpty()
                },
                async {
                    request.filter.auctions?.let {
                        enrichmentAuctionService.fetchAuctionsIfAbsent(it.toSet(), emptyMap()).values
                    }.orEmpty()
                }
            )
        }

        val (cursor, entities) = elasticHelper.getRawOwnershipsBySearchRequest(request)
        return apiHelper.getEnrichedOwnerships(
            continuation = request.continuation,
            size = request.size,
            { auctions.awaitAll().flatten() },
            { entities },
            { ownerships ->
                OwnershipsDto(
                    continuation = cursor,
                    ownerships = apiHelper.enrich(ownerships)
                )
            }
        )
    }
}
