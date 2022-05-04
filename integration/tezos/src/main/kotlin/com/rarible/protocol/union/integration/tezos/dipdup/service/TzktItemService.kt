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
        throw UnionNotFoundException(null)
    }

    suspend fun getItemById(
        itemId: String
    ): UnionItem {
        throw UnionNotFoundException(null)
    }

    suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        throw UnionNotFoundException(null)
    }

    suspend fun getItemMetaById(itemId: String): UnionMeta {
        throw UnionNotFoundException(null)
    }

    suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        throw UnionNotFoundException(null)
    }

}
