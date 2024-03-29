package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.dipdup.client.TokenClient
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupItemConverter

class DipDupItemService(
    private val dipdupTokenClient: TokenClient,
) : DipDupService {

    suspend fun getAllItems(
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val page = dipdupTokenClient.getTokensAll(
            limit = size,
            showDeleted = false,
            continuation = continuation,
            sortAsc = false
        )
        return Page(
            total = page.items.size.toLong(),
            continuation = page.continuation,
            entities = page.items.map { DipDupItemConverter.convert(it) }
        )
    }

    suspend fun getItemsByOwner(
        address: String,
        size: Int,
        continuation: String?
    ): Page<UnionItem> {
        val page = dipdupTokenClient.getTokensByOwner(address, size, continuation)
        return Page(
            total = page.items.size.toLong(),
            continuation = page.continuation,
            entities = page.items.map { DipDupItemConverter.convert(it) }
        )
    }

    suspend fun getItemsByCreator(
        address: String,
        size: Int,
        continuation: String?
    ): Page<UnionItem> {
        val page = dipdupTokenClient.getTokensByCreator(address, size, continuation)
        return Page(
            total = page.items.size.toLong(),
            continuation = page.continuation,
            entities = page.items.map { DipDupItemConverter.convert(it) }
        )
    }

    suspend fun getItemsByCollection(
        address: String,
        size: Int,
        continuation: String?
    ): Page<UnionItem> {
        val page = dipdupTokenClient.getTokensByCollection(address, size, continuation)
        return Page(
            total = page.items.size.toLong(),
            continuation = page.continuation,
            entities = page.items.map { DipDupItemConverter.convert(it) }
        )
    }

    suspend fun getItemById(
        itemId: String
    ): UnionItem {
        val item = safeApiCall("Item $itemId wasn't found") { dipdupTokenClient.getTokenById(itemId) }
        return DipDupItemConverter.convert(item)
    }

    suspend fun getMetaById(
        itemId: String
    ): UnionMeta {
        val meta = safeApiCall("Meta wasn't found for item $itemId") { dipdupTokenClient.getTokenMetaById(itemId) }
        return DipDupItemConverter.convertMeta(meta)
    }

    suspend fun resetMetaById(itemId: String) {
        dipdupTokenClient.removeTokenMetaById(itemId)
    }

    suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        val items = dipdupTokenClient.getTokensByIds(itemIds)
        return items.map { DipDupItemConverter.convert(it) }
    }
}
