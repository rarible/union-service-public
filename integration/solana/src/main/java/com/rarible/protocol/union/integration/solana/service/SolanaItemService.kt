package com.rarible.protocol.union.integration.solana.service

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.solana.converter.SolanaItemConverter
import com.rarible.protocol.union.integration.solana.converter.SolanaItemMetaConverter
import com.rarible.solana.protocol.api.client.TokenControllerApi
import kotlinx.coroutines.reactive.awaitFirst

class SolanaItemService(
    private val tokenApi: TokenControllerApi,
) : AbstractBlockchainService(BlockchainDto.SOLANA), ItemService {

    override suspend fun getItemById(itemId: String): UnionItem {
        val token = tokenApi.getTokenByAddress(itemId).awaitFirst()
        return SolanaItemConverter.convert(token, blockchain)
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        val tokenMeta = tokenApi.getTokenMetaByAddress(itemId).awaitFirst()
        return SolanaItemMetaConverter.convert(tokenMeta)
    }

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        TODO("Not yet implemented")
    }

    override suspend fun resetItemMeta(itemId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsByCreator(creator: String, continuation: String?, size: Int): Page<UnionItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsByOwner(owner: String, continuation: String?, size: Int): Page<UnionItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        TODO("Not yet implemented")
    }
}
