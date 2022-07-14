package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page

interface TzktItemService {

    fun enabled() = false

    suspend fun getAllItems(
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        TODO("Not implemented")
    }

    suspend fun getItemById(
        itemId: String
    ): UnionItem {
        TODO("Not implemented")
    }

    suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        TODO("Not implemented")
    }

    suspend fun getItemMetaById(itemId: String): UnionMeta {
        TODO("Not implemented")
    }

    suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        TODO("Not implemented")
    }

    suspend fun getItemsByOwner(owner: String, continuation: String?, size: Int): Page<UnionItem> {
        TODO("Not implemented")
    }

    suspend fun getItemsByCreator(creator: String, continuation: String?, size: Int): Page<UnionItem> {
        TODO("Not implemented")
    }

    suspend fun getItemsByCollection(collection: String, continuation: String?, size: Int): Page<UnionItem> {
        TODO("Not implemented")
    }

    suspend fun isNft(itemId: String): Boolean {
        TODO("Not implemented")
    }

}
