package com.rarible.protocol.union.integration.tezos.service

import com.rarible.protocol.tezos.api.client.NftItemControllerApi
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.TezosComponent
import com.rarible.protocol.union.integration.tezos.converter.TezosItemConverter
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component

@Component
@TezosComponent
class TezosItemService(
    private val itemControllerApi: NftItemControllerApi
) : AbstractBlockchainService(BlockchainDto.TEZOS), ItemService {

    private val WITH_META = true

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItem> {
        // TODO TEZOS implement
        /* val items = itemControllerApi.getNftAllItems(
             continuation,
             size,
             showDeleted,
             lastUpdatedFrom,
             lastUpdatedTo
         ).awaitFirst()
         return TezosItemConverter.convert(items, blockchain)*/
        return Page(0, null, emptyList())
    }

    override suspend fun getItemById(
        itemId: String
    ): UnionItem {
        val item = itemControllerApi.getNftItemById(
            itemId, WITH_META
        ).awaitFirst()
        return TezosItemConverter.convert(item, blockchain)
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        val meta = itemControllerApi.getNftItemMetaById(itemId).awaitFirst()
        return TezosItemConverter.convert(meta)
    }

    override suspend fun resetItemMeta(itemId: String) {
        // TODO TEZOS implement
        //itemControllerApi.resetNftItemMetaById(itemId)
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = itemControllerApi.getNftItemsByCollection(
            collection,
            WITH_META,
            size,
            continuation
        ).awaitFirst()
        return TezosItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = itemControllerApi.getNftItemsByCreator(
            creator,
            WITH_META,
            size,
            continuation
        ).awaitFirst()
        return TezosItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = itemControllerApi.getNftItemsByOwner(
            owner,
            WITH_META,
            size,
            continuation
        ).awaitFirst()
        return TezosItemConverter.convert(items, blockchain)
    }

}
