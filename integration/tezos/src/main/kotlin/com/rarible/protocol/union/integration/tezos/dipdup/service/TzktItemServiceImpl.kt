package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktItemConverter
import com.rarible.tzkt.client.TokenClient
import com.rarible.tzkt.model.TzktNotFound
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class TzktItemServiceImpl(val tzktTokenClient: TokenClient, val properties: DipDupIntegrationProperties) :
    TzktItemService {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val blockchain = BlockchainDto.TEZOS

    override fun enabled() = true

    override suspend fun getAllItems(continuation: String?, size: Int): Page<UnionItem> {
        val tzktPage = tzktTokenClient.allTokensByLastUpdate(size = size, continuation = continuation)
        return TzktItemConverter.convert(tzktPage, blockchain)
    }

    override suspend fun getItemById(itemId: String): UnionItem {
        val token = safeApiCall { tzktTokenClient.token(itemId) }
        return TzktItemConverter.convert(token, blockchain)
    }

    override suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        val tokens = safeApiCall { tzktTokenClient.tokens(itemIds) }
        return tokens.map { TzktItemConverter.convert(it, blockchain) }
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        val meta = safeApiCall { tzktTokenClient.tokenMeta(itemId) }
        return TzktItemConverter.convert(meta, blockchain)
    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        val royalty = safeApiCall { tzktTokenClient.royalty(itemId) }
        return TzktItemConverter.convert(royalty, blockchain)
    }

    override suspend fun isNft(itemId: String): Boolean {
        var retries = 0

        while (retries++ < properties.tzktProperties.retryAttempts) {
            val result = tzktTokenClient.isNft(itemId)
            result?.let { return it } ?: coroutineScope { delay(properties.tzktProperties.retryDelay) }
        }
        return false
    }

    private suspend fun <T> safeApiCall(clientCall: suspend () -> T): T {
        return try {
            clientCall()
        } catch (e: TzktNotFound) {
            throw UnionNotFoundException(message = e.message ?: "")
        }
    }

}
