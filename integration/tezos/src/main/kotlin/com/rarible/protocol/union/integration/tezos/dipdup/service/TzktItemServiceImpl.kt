package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktItemConverter
import com.rarible.tzkt.client.TokenClient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class TzktItemServiceImpl(val tzktTokenClient: TokenClient, val properties: DipDupIntegrationProperties): TzktItemService {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val blockchain = BlockchainDto.TEZOS

    override fun enabled() = true

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

    override suspend fun isNft(itemId: String): Boolean {
        var retries = 0

        while (retries++ < properties.tzktProperties.retryAttempts) {
            val result = tzktTokenClient.isNft(itemId)
            result?.let { return it } ?: coroutineScope { delay(properties.tzktProperties.retryDelay) }
        }
        return false
    }

}
