package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.exception.FeatureUnderConstructionException
import com.rarible.protocol.union.api.service.elastic.CollectionElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.CollectionsSearchRequestDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.service.query.collection.CollectionQueryService
import org.springframework.stereotype.Service

@Service
class CollectionSourceSelectService(
    private val featureFlagsProperties: FeatureFlagsProperties,
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
        owner: UnionAddress,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): CollectionsDto {
        return getQuerySource().getCollectionsByOwner(owner, blockchains, continuation, size)
    }

    suspend fun searchCollections(collectionsSearchRequestDto: CollectionsSearchRequestDto): CollectionsDto {
        return if (featureFlagsProperties.enableSearchCollections) {
            collectionElasticService.searchCollections(collectionsSearchRequestDto)
        } else throw FeatureUnderConstructionException("searchCollections() feature is under construction")
    }

    private fun getQuerySource(): CollectionQueryService {
        return collectionElasticService
    }
}
