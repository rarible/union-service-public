package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.tezos.api.client.NftItemControllerApi
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.converter.TezosItemConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException

@CaptureSpan(type = "blockchain")
open class TezosItemService(
    private val itemControllerApi: NftItemControllerApi,
    private val tzktItemService: TzktItemService
) : AbstractBlockchainService(BlockchainDto.TEZOS), ItemService {

    private val WITH_META = true

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItem> {
        return if (tzktItemService.enabled()) {
            // others params are not implemented in tzkt yet
            tzktItemService.getAllItems(continuation, size)
        } else {
            val items = itemControllerApi.getNftAllItems(
                lastUpdatedFrom?.toString(),
                lastUpdatedTo?.toString(),
                showDeleted,
                WITH_META,
                size,
                continuation
            ).awaitFirst()
            TezosItemConverter.convert(items, blockchain)
        }
    }

    override suspend fun getItemById(
        itemId: String
    ): UnionItem {
        return if (tzktItemService.enabled()) {
            tzktItemService.getItemById(itemId)
        } else {
            val item = itemControllerApi.getNftItemById(
                itemId, WITH_META
            ).awaitFirst()
            TezosItemConverter.convert(item, blockchain)
        }
    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        try {
            val royalties = itemControllerApi.getNftItemRoyalties(itemId).awaitFirst()
            return royalties.royalties.map { TezosItemConverter.toRoyalty(it, blockchain) }
        } catch (e: WebClientResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return emptyList()
            }
            throw e
        }
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        return if (tzktItemService.enabled()) {
            tzktItemService.getItemMetaById(itemId)
        } else {
            val meta = itemControllerApi.getNftItemMetaById(itemId).awaitFirst()
            TezosItemConverter.convert(meta)
        }
    }

    override suspend fun resetItemMeta(itemId: String) {
        itemControllerApi.resetNftItemMetaById(itemId).awaitFirstOrNull()
    }

    override suspend fun getItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = itemControllerApi.getNftItemsByCollection(
            collection,
            WITH_META,
            size,
            continuation
        ).awaitFirst()
        return TezosItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = itemControllerApi.getNftItemsByCreator(
            creator,
            WITH_META,
            size,
            continuation
        ).awaitFirst()
        return TezosItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = itemControllerApi.getNftItemsByOwner(
            owner,
            WITH_META,
            size,
            continuation
        ).awaitFirst()
        return TezosItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        return if (tzktItemService.enabled()) {
            tzktItemService.getItemsByIds(itemIds)
        } else {
            // Not implemented in legacy indexer
            emptyList()
        }
    }
}
