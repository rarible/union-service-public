package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.OwnershipQueryService
import com.rarible.protocol.union.api.service.api.OwnershipApiQueryService
import com.rarible.protocol.union.api.service.elastic.OwnershipElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Slice
import org.springframework.stereotype.Service

@Service
class OwnershipSourceSelectService(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val ownershipApiQueryService: OwnershipApiQueryService,
    private val ownershipElasticService: OwnershipElasticService
) {

    suspend fun getOwnershipById(fullOwnershipId: OwnershipIdDto): OwnershipDto {
        return ownershipApiQueryService.getOwnershipById(fullOwnershipId)
    }

    suspend fun getOwnershipsByIds(ids: List<OwnershipIdDto>): List<OwnershipDto> {
        return getQuerySource().getOwnershipsByIds(ids)
    }

    suspend fun getOwnershipByOwner(
        owner: UnionAddress,
        continuation: String?,
        size: Int
    ): Slice<UnionOwnership> {
        return getQuerySource().getOwnershipByOwner(owner, continuation, size)
    }

    suspend fun getOwnershipsByItem(itemId: ItemIdDto, continuation: String?, size: Int): OwnershipsDto {
        return getQuerySource().getOwnershipsByItem(itemId, continuation, size)
    }

    private fun getQuerySource(): OwnershipQueryService {
        return when (featureFlagsProperties.enableOwnershipQueriesToElasticSearch) {
            true -> ownershipElasticService
            else -> ownershipApiQueryService
        }
    }
}