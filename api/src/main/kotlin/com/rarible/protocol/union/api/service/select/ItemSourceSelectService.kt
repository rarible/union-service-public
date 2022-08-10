package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.exception.FeatureUnderConstructionException
import com.rarible.protocol.union.api.service.elastic.ItemElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.ItemsSearchRequestDto
import com.rarible.protocol.union.dto.ItemsWithOwnershipDto
import com.rarible.protocol.union.dto.SearchEngineDto
import com.rarible.protocol.union.enrichment.service.query.item.ItemApiMergeService
import com.rarible.protocol.union.enrichment.service.query.item.ItemQueryService
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class ItemSourceSelectService(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val itemApiService: ItemApiMergeService,
    private val itemElasticService: ItemElasticService
) {

    suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?,
        searchEngine: SearchEngineDto?
    ): ItemsDto {
        return getQuerySource(searchEngine).getAllItems(
            blockchains,
            continuation,
            size,
            showDeleted,
            lastUpdatedFrom,
            lastUpdatedTo
        )
    }

    suspend fun getAllItemIdsByCollection(
        collectionId: CollectionIdDto,
        searchEngine: SearchEngineDto?
    ): Flow<ItemIdDto> {
        return getQuerySource(searchEngine).getAllItemIdsByCollection(collectionId)
    }

    suspend fun getItemsByIds(ids: List<ItemIdDto>): List<ItemDto> {
        return itemApiService.getItemsByIds(ids)
    }

    suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?,
        searchEngine: SearchEngineDto?
    ): ItemsDto {
        return getQuerySource(searchEngine).getItemsByCollection(
            collection = collection,
            continuation = continuation,
            size = size
        )
    }

    suspend fun getItemsByCreator(
        creator: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        searchEngine: SearchEngineDto?
    ): ItemsDto {
        return getQuerySource(searchEngine).getItemsByCreator(
            creator = creator,
            blockchains = blockchains,
            continuation = continuation,
            size = size
        )
    }

    suspend fun getItemsByOwner(
        owner: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        searchEngine: SearchEngineDto?
    ): ItemsDto {

        return getQuerySource(searchEngine).getItemsByOwner(
            owner = owner,
            blockchains = blockchains,
            continuation = continuation,
            size = size
        )
    }

    suspend fun getItemsByOwnerWithOwnership(
        owner: String,
        continuation: String?,
        size: Int?,
        searchEngine: SearchEngineDto?
    ): ItemsWithOwnershipDto {
        return getQuerySource(searchEngine).getItemsByOwnerWithOwnership(owner, continuation, size)
    }

    suspend fun searchItems(itemsSearchRequestDto: ItemsSearchRequestDto): ItemsDto {
        return if (featureFlagsProperties.enableItemQueriesToElasticSearch) {
            itemElasticService.searchItems(itemsSearchRequestDto)
        } else throw FeatureUnderConstructionException("searchItems() feature is under construction")
    }

    private fun getQuerySource(searchEngine: SearchEngineDto?): ItemQueryService {
        if (searchEngine != null) {
            return when (searchEngine) {
                SearchEngineDto.LEGACY -> itemApiService
                SearchEngineDto.V1 -> itemApiService
            }
        }
        return when (featureFlagsProperties.enableItemQueriesToElasticSearch) {
            true -> itemElasticService
            else -> itemApiService
        }
    }

}
