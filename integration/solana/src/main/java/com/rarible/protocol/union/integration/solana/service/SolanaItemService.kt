package com.rarible.protocol.union.integration.solana.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.solana.api.client.TokenControllerApi
import com.rarible.protocol.solana.dto.TokenIdsDto
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdsDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.solana.converter.SolanaItemConverter
import com.rarible.protocol.union.integration.solana.converter.SolanaItemMetaConverter
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
        val page = tokenApi.getAllTokens(
            showDeleted,
            lastUpdatedFrom,
            lastUpdatedTo,
            continuation,
            size
        ).awaitFirst()

        return SolanaItemConverter.convert(page, blockchain)
    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        val royaltyList = tokenApi.getTokenRoyaltiesByAddress(itemId).awaitFirst()
        val royalties = royaltyList.royalties
        return royalties.map { SolanaItemConverter.convert(it, blockchain) }
    }

    override suspend fun resetItemMeta(itemId: String) = Unit

    override suspend fun getItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val page = tokenApi.getTokensByCollection(
            collection,
            continuation,
            size
        ).awaitFirst()

        return SolanaItemConverter.convert(page, blockchain)
    }

    override suspend fun getItemsByCreator(creator: String, continuation: String?, size: Int): Page<UnionItem> {
        val page = tokenApi.getTokensByCreator(
            creator,
            continuation,
            size
        ).awaitFirst()

        return SolanaItemConverter.convert(page, blockchain)
    }

    override suspend fun getItemsByOwner(owner: String, continuation: String?, size: Int): Page<UnionItem> {
        val page = tokenApi.getTokensByOwner(
            owner,
            continuation,
            size
        ).awaitFirst()

        return SolanaItemConverter.convert(page, blockchain)
    }

    override suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        val tokensDto = tokenApi.getTokensByAddresses(TokenIdsDto(itemIds)).awaitFirst()

        return tokensDto.tokens.map { SolanaItemConverter.convert(it, blockchain) }
    }

    override suspend fun getItemCollectionId(itemId: String): String? {
        return getItemById(itemId).collection?.value
    }
}
