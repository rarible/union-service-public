package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.page.ArgPage
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ItemQueryService {

    suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        cursor: String?,
        safeSize: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): List<ArgPage<UnionItem>>

    suspend fun getAllItemIdsByCollection(collectionId: CollectionIdDto): Flow<ItemIdDto>
    suspend fun getItemsByIds(ids: List<ItemIdDto>): List<ItemDto>
    suspend fun enrich(unionItemsPage: Page<UnionItem>): ItemsDto
    suspend fun enrich(unionItemsSlice: Slice<UnionItem>, total: Long): ItemsDto
    suspend fun enrich(unionItem: UnionItem): ItemDto
}
