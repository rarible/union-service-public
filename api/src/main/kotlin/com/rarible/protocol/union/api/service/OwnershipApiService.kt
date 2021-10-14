package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.UnionOwnershipDto
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class OwnershipApiService(
    private val orderApiService: OrderApiService,
    private val enrichmentOwnershipService: EnrichmentOwnershipService
) {

    suspend fun enrich(unionOwnershipsPage: Page<UnionOwnershipDto>): OwnershipsDto {
        return OwnershipsDto(
            total = unionOwnershipsPage.total,
            continuation = unionOwnershipsPage.continuation,
            ownerships = enrich(unionOwnershipsPage.entities)
        )
    }

    suspend fun enrich(unionOwnership: UnionOwnershipDto): OwnershipDto {
        val shortId = ShortOwnershipId(unionOwnership.id)
        val shortOwnership = enrichmentOwnershipService.get(shortId)
        if (shortOwnership == null) {
            return EnrichedOwnershipConverter.convert(unionOwnership)
        }
        return enrichmentOwnershipService.enrichOwnership(shortOwnership, unionOwnership)
    }

    private suspend fun enrich(unionOwnerships: List<UnionOwnershipDto>): List<OwnershipDto> {
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

        return result
    }
}
