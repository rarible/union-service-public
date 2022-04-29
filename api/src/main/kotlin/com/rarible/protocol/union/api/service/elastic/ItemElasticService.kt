package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.api.service.ItemQueryService
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.continuation.page.ArgPage
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
@CaptureSpan(type = SpanType.APP)
class ItemElasticService : ItemQueryService {

    override suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        cursor: String?,
        safeSize: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): List<ArgPage<UnionItem>> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllItemIdsByCollection(collectionId: CollectionIdDto): Flow<ItemIdDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsByIds(ids: List<ItemIdDto>): List<ItemDto> {
        throw NotImplementedError()
    }

    override suspend fun enrich(unionItemsPage: Page<UnionItem>): ItemsDto {
        throw NotImplementedError()
    }

    override suspend fun enrich(unionItemsSlice: Slice<UnionItem>, total: Long): ItemsDto {
        throw NotImplementedError()
    }

    override suspend fun enrich(unionItem: UnionItem): ItemDto {
        throw NotImplementedError()
    }
}