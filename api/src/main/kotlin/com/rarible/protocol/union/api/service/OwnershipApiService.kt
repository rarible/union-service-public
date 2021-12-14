package com.rarible.protocol.union.api.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.continuation.CombinedContinuation
import com.rarible.protocol.union.core.continuation.page.ArgPage
import com.rarible.protocol.union.core.continuation.page.ArgSlice
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.util.spent
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
    private val router: BlockchainRouter<OwnershipService>,
    private val enrichmentOwnershipService: EnrichmentOwnershipService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getAllOwnerships(
        blockchains: List<BlockchainDto>?,
        cursor: String?,
        safeSize: Int,
    ): List<ArgPage<UnionOwnership>> {
        val evaluatedBlockchains = router.getEnabledBlockChains(blockchains).map(BlockchainDto::name)
        val slices = getOwnersByBlockchains(cursor, evaluatedBlockchains) { blockchain, continuation ->
            val blockDto = BlockchainDto.valueOf(blockchain)
            router.getService(blockDto).getAllOwnerships(continuation, safeSize)
        }
        return slices
    }

    suspend fun enrich(slice: Slice<UnionOwnership>, total: Long): OwnershipsDto? {
        val now = nowMillis()
        val result = OwnershipsDto(
            total = total,
            continuation = slice.continuation,
            ownerships = enrich(slice.entities)
        )
        logger.info("Enriched {} ownerships ({}ms)", slice.entities.size, spent(now))
        return result
    }

    suspend fun enrich(unionOwnershipsPage: Page<UnionOwnership>): OwnershipsDto {
        val now = nowMillis()
        val result = OwnershipsDto(
            total = unionOwnershipsPage.total,
            continuation = unionOwnershipsPage.continuation,
            ownerships = enrich(unionOwnershipsPage.entities)
        )
        logger.info("Enriched {} ownerships ({}ms)", unionOwnershipsPage.entities.size, spent(now))
        return result
    }

    suspend fun enrich(unionOwnership: UnionOwnership): OwnershipDto {
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

        val now = nowMillis()

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

        logger.info(
            "Enriched {} of {} Ownerships, {} Orders fetched ({}ms)",
            existingEnrichedOwnerships.size, result.size, orders.size, spent(now)
        )

        return result
    }

    private suspend fun getOwnersByBlockchains(
        continuation: String?,
        blockchains: Collection<String>,
        clientCall: suspend (blockchain: String, continuation: String?) -> Page<UnionOwnership>
    ): List<ArgPage<UnionOwnership>> {
        val currentContinuation = CombinedContinuation.parse(continuation)
        return coroutineScope {
            blockchains.map { blockchain ->
                async {
                    val blockchainContinuation = currentContinuation.continuations[blockchain]
                    // For completed blockchain we do not request orders
                    if (blockchainContinuation == ArgSlice.COMPLETED) {
                        ArgPage(blockchain, blockchainContinuation, Page(0, null, emptyList()))
                    } else {
                        ArgPage(
                            blockchain,
                            blockchainContinuation,
                            clientCall.invoke(blockchain, blockchainContinuation)
                        )
                    }
                }
            }
        }.awaitAll()
    }
}
