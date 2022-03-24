package com.rarible.protocol.union.integration.solana.service

import com.rarible.core.apm.CaptureSpan
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

@CaptureSpan(type = "blockchain")
open class SolanaItemService(
    private val tokenApi: TokenControllerApi
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
        // TODO SOLANA implement
        return Page.empty()
    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        // TODO SOLANA implement
        return emptyList()
    }

    override suspend fun resetItemMeta(itemId: String) {
        // TODO SOLANA implement
    }

    override suspend fun getItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val tokensDto = tokenApi.getTokensByCollection(collection).awaitFirst()

        return Page(
            total = tokensDto.total,
            null, // TODO SOLANA add continuation,
            tokensDto.tokens.map { SolanaItemConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getItemsByCreator(creator: String, continuation: String?, size: Int): Page<UnionItem> {
        return Page.empty() // TODO SOLANA implement
    }

    override suspend fun getItemsByOwner(owner: String, continuation: String?, size: Int): Page<UnionItem> {
        val tokensDto = tokenApi.getTokensByOwner(owner).awaitFirst()

        return Page(
            total = tokensDto.total,
            null, // TODO SOLANA add continuation,
            tokensDto.tokens.map { SolanaItemConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        val tokensDto = tokenApi.getTokensByAddresses(itemIds).awaitFirst()

        return tokensDto.tokens.map { SolanaItemConverter.convert(it, blockchain) }
    }
}
