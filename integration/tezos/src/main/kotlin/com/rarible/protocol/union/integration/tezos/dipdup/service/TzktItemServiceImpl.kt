package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktItemConverter
import com.rarible.tzkt.client.TokenClient
import com.rarible.tzkt.model.ItemId
import com.rarible.tzkt.model.TzktNotFound
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class TzktItemServiceImpl(val tzktTokenClient: TokenClient, val properties: DipDupIntegrationProperties) :
    TzktItemService {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val blockchain = BlockchainDto.TEZOS

    override fun enabled() = true

    override suspend fun getAllItems(continuation: String?, size: Int): Page<UnionItem> {
        val tzktPage = tzktTokenClient.allTokensByLastUpdate(
            size = size,
            continuation = continuation,
            sortAsc = false,
            loadMeta = false
        )
        return TzktItemConverter.convert(tzktPage, blockchain)
    }

    override suspend fun getItemById(itemId: String): UnionItem {
        val token = safeApiCall { tzktTokenClient.token(itemId) }
        return TzktItemConverter.convert(token, blockchain).copy(creators = creators(itemId))
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

    override suspend fun getItemsByCollection(collection: String, continuation: String?, size: Int): Page<UnionItem> {
        val tzktPage = tzktTokenClient.tokensByCollection(collection, size, continuation)
        return TzktItemConverter.convert(tzktPage, blockchain)
    }

    override suspend fun getItemsByCreator(creator: String, continuation: String?, size: Int): Page<UnionItem> {
        val tzktPage = tzktTokenClient.tokensByCreator(creator, size, continuation)
        return TzktItemConverter.convert(tzktPage, blockchain)
    }

    override suspend fun getItemsByOwner(owner: String, continuation: String?, size: Int): Page<UnionItem> {
        val tzktPage = tzktTokenClient.tokensByOwner(owner, size, continuation)
        return TzktItemConverter.convert(tzktPage, blockchain)
    }

    override suspend fun isNft(itemId: String): Boolean {

        // check fungible list first
        val parsed = ItemId.parse(itemId)
        if (properties.fungibleContracts.contains(parsed.contract)) {
            return false
        }

        if (!properties.tzktProperties.nftChecking) {
            return true
        }

        var retries = 0

        // meta is loaded asynchronously with delay that's why we should retry to check if it's nft
        while (retries++ < properties.tzktProperties.retryAttempts) {

            val token = tzktTokenClient.token(itemId)
            val ignoreDate = OffsetDateTime.now().minus(properties.tzktProperties.ignorePeriod, ChronoUnit.MILLIS)
            val isNft = token.isNft()

            // if token is new we wait and retry
            if (!isNft && token.lastTime!! > ignoreDate && retries < properties.tzktProperties.retryAttempts) {
                coroutineScope { delay(properties.tzktProperties.retryDelay) }
            } else {
                return isNft
            }
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

    private suspend fun creators(itemId: String): List<CreatorDto> {
        return try {
            val royalty = safeApiCall { tzktTokenClient.royalty(itemId) }.map { CreatorDto(UnionAddress(blockchain.group(), it.address), it.share) }
            if (royalty.isNotEmpty()) royalty.subList(0, 1)
            else emptyList()
        } catch (ex: Exception) {
            logger.error("Failed to get royalty for setting creators $itemId", ex)
            emptyList()
        }
    }

}
