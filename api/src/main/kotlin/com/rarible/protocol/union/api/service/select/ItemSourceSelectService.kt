package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.ItemApiService
import com.rarible.protocol.union.api.service.ItemQueryService
import com.rarible.protocol.union.api.service.elastic.ItemElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.continuation.page.ArgPage
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@ExperimentalCoroutinesApi
@Service
class ItemSourceSelectService(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val itemApiService: ItemApiService,
    private val itemElasticService: ItemElasticService
) : ItemQueryService {
    override suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        cursor: String?,
        safeSize: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): List<ArgPage<UnionItem>> {
        return getQuerySource().getAllItems(blockchains, cursor, safeSize, showDeleted, lastUpdatedFrom, lastUpdatedTo)
    }

    override suspend fun getAllItemIdsByCollection(collectionId: CollectionIdDto): Flow<ItemIdDto> {
        return getQuerySource().getAllItemIdsByCollection(collectionId)
    }

    override suspend fun getItemsByIds(ids: List<ItemIdDto>): List<ItemDto> {
        return itemApiService.getItemsByIds(ids)
    }

    override suspend fun enrich(unionItemsPage: Page<UnionItem>): ItemsDto {
        return itemApiService.enrich(unionItemsPage)
    }

    override suspend fun enrich(unionItemsSlice: Slice<UnionItem>, total: Long): ItemsDto {
        return itemApiService.enrich(unionItemsSlice, total)
    }

    override suspend fun enrich(unionItem: UnionItem): ItemDto {
        return itemApiService.enrich(unionItem)
    }

    private fun getQuerySource(): ItemQueryService {
        return when (featureFlagsProperties.enableItemQueriesToElasticSearch) {
            true -> itemElasticService
            else -> itemApiService
        }
    }
}