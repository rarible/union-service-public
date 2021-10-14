package com.rarible.protocol.union.core.tezos.service

import com.rarible.protocol.tezos.api.client.NftItemControllerApi
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.tezos.converter.TezosItemConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.UnionItemDto
import kotlinx.coroutines.reactive.awaitFirst

class TezosItemService(
    blockchain: BlockchainDto,
    private val itemControllerApi: NftItemControllerApi
) : AbstractBlockchainService(blockchain), ItemService {

    private val WITH_META = true

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItemDto> {
        // TODO TEZOS Not implemented
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
    ): UnionItemDto {
        val item = itemControllerApi.getNftItemById(
            itemId, WITH_META
        ).awaitFirst()
        return TezosItemConverter.convert(item, blockchain)
    }

    override suspend fun getItemMetaById(itemId: String): MetaDto {
        val meta = itemControllerApi.getNftItemMetaById(itemId).awaitFirst()
        return TezosItemConverter.convert(meta)
    }

    override suspend fun resetItemMeta(itemId: String) {
        // TODO TEZOS Not implemented
        //itemControllerApi.resetNftItemMetaById(itemId)
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int
    ): Page<UnionItemDto> {
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
    ): Page<UnionItemDto> {
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
    ): Page<UnionItemDto> {
        val items = itemControllerApi.getNftItemsByOwner(
            owner,
            WITH_META,
            size,
            continuation
        ).awaitFirst()
        return TezosItemConverter.convert(items, blockchain)
    }

}
