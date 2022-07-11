package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.enrichment.service.query.item.ItemApiMergeService
import com.rarible.protocol.union.enrichment.service.query.item.ItemQueryService
import com.rarible.protocol.union.api.service.elastic.ItemElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.ItemsWithOwnershipDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        lastUpdatedTo: Long?
    ): ItemsDto {
        return getQuerySource().getAllItems(
            blockchains,
            continuation,
            size,
            showDeleted,
            lastUpdatedFrom,
            lastUpdatedTo
        )
    }

    suspend fun getAllItemIdsByCollection(collectionId: CollectionIdDto): Flow<ItemIdDto> {
        return getQuerySource().getAllItemIdsByCollection(collectionId)
    }

    suspend fun getItemsByIds(ids: List<ItemIdDto>): List<ItemDto> {
        return itemApiService.getItemsByIds(ids)
    }

    suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        return getQuerySource().getItemsByCollection(collection = collection, continuation = continuation, size = size)
    }

    suspend fun getItemsByCreator(
        creator: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        return getQuerySource().getItemsByCreator(
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
        size: Int?
    ): ItemsDto {

        return getQuerySource().getItemsByOwner(
            owner = owner,
            blockchains = blockchains,
            continuation = continuation,
            size = size
        )
    }

    suspend fun getItemsByOwnerWithOwnership(
        owner: String,
        continuation: String?,
        size: Int?
    ): ItemsWithOwnershipDto {
        return getQuerySource().getItemsByOwnerWithOwnership(owner, continuation, size)
    }

    private fun getQuerySource(): ItemQueryService {
        return when (featureFlagsProperties.enableItemQueriesToElasticSearch) {
            true -> itemElasticService
            else -> itemApiService
        }
    }
}