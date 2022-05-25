package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.api.CollectionApiMergeService
import com.rarible.protocol.union.api.service.CollectionQueryService
import com.rarible.protocol.union.api.service.elastic.CollectionElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionsDto
import org.springframework.stereotype.Service

@Service
class CollectionSourceSelectService(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val collectionApiMergeService: CollectionApiMergeService,
    private val collectionElasticService: CollectionElasticService,
) : CollectionQueryService {

    override suspend fun getAllCollections(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): CollectionsDto {
        return getQuerySource().getAllCollections(blockchains, continuation, size)
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): CollectionsDto {
        return getQuerySource().getCollectionsByOwner(owner, blockchains, continuation, size)
    }

    private fun getQuerySource(): CollectionQueryService {
        return when (featureFlagsProperties.enableCollectionQueriesToElastic) {
            true -> collectionElasticService
            else -> collectionApiMergeService
        }
    }
}
