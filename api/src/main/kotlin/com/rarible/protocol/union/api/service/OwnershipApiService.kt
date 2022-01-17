package com.rarible.protocol.union.api.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class OwnershipApiService(
    private val orderApiService: OrderApiService,
    private val enrichmentOwnershipService: EnrichmentOwnershipService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

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
}
