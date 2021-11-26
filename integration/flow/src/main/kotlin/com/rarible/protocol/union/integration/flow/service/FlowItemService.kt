package com.rarible.protocol.union.integration.flow.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.model.UnionMedia
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.integration.flow.converter.FlowItemConverter
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException

@CaptureSpan(type = "blockchain")
open class FlowItemService(
    private val flowNftItemControllerApi: FlowNftItemControllerApi
) : AbstractBlockchainService(BlockchainDto.FLOW), ItemService {

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItem> {
        val items = flowNftItemControllerApi
            .getNftAllItems(continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo)
            .awaitFirst()
        return FlowItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemById(itemId: String): UnionItem {
        val item = flowNftItemControllerApi.getNftItemById(itemId).awaitFirst()
        return FlowItemConverter.convert(item, blockchain)
    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        // TODO FLOW implement
        try {
            return getItemById(itemId).royalties
        } catch (e: WebClientResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return emptyList()
            }
            throw e
        }
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        val meta = flowNftItemControllerApi.getNftItemMetaById(itemId).awaitFirst()
        return FlowItemConverter.convert(meta)
    }

    override suspend fun getItemImageById(itemId: String): UnionMedia {
        TODO("Not yet implemented")
    }

    override suspend fun getItemAnimationById(itemId: String): UnionMedia {
        TODO("Not yet implemented")
    }

    override suspend fun resetItemMeta(itemId: String) {
        flowNftItemControllerApi.resetItemMeta(itemId).awaitFirstOrNull()
    }

    override suspend fun getItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = flowNftItemControllerApi.getNftItemsByCollection(
            collection,
            continuation,
            size
        ).awaitFirst()
        return FlowItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = flowNftItemControllerApi.getNftItemsByCreator(
            creator,
            continuation,
            size
        ).awaitFirst()
        return FlowItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = flowNftItemControllerApi.getNftItemsByOwner(
            owner,
            continuation,
            size
        ).awaitFirst()
        return FlowItemConverter.convert(items, blockchain)
    }

}
