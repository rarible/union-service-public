package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.OwnershipQueryService
import com.rarible.protocol.union.api.service.api.OwnershipApiService
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
    private val ownershipApiService: OwnershipApiService,
    private val ownershipElasticService: OwnershipElasticService,
) : OwnershipQueryService {
    override suspend fun getOwnershipById(fullOwnershipId: OwnershipIdDto): OwnershipDto =
        getQuerySource().getOwnershipById(fullOwnershipId)

    override suspend fun getOwnershipByOwner(
        owner: UnionAddress,
        continuation: String?,
        size: Int,
    ): Slice<UnionOwnership> =
        getQuerySource().getOwnershipByOwner(owner, continuation, size)

    override suspend fun getOwnershipsByItem(itemId: ItemIdDto, continuation: String?, size: Int): OwnershipsDto =
        getQuerySource().getOwnershipsByItem(itemId, continuation, size)

    private fun getQuerySource(): OwnershipQueryService {
        return when (featureFlagsProperties.enableOrderQueriesToElasticSearch) {
            true -> ownershipElasticService
            else -> ownershipApiService
        }
    }
}
