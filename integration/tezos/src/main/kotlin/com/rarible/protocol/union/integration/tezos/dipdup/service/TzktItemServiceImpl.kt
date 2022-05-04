package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktItemConverter
import com.rarible.tzkt.client.TokenClient

class TzktItemServiceImpl(val tzktTokenClient: TokenClient): TzktItemService {

    override fun enabled() = true

    private val blockchain = BlockchainDto.TEZOS

    override suspend fun getAllItems(continuation: String?, size: Int): Page<UnionItem> {
        val tzktPage = tzktTokenClient.tokens(size = size, continuation = continuation)
        return TzktItemConverter.convert(tzktPage, blockchain)
    }

    override suspend fun getItemById(itemId: String): UnionItem {
        return TzktItemConverter.convert(tzktTokenClient.token(itemId), blockchain)
    }

    override suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        return tzktTokenClient.tokens(itemIds).map { TzktItemConverter.convert(it, blockchain) }
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        return TzktItemConverter.convert(tzktTokenClient.tokenMeta(itemId), blockchain)
    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        return TzktItemConverter.convert(tzktTokenClient.royalty(itemId), blockchain)
    }

}
