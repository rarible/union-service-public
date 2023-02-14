package com.rarible.protocol.union.integration.flow.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.FlowItemIdsDto
import com.rarible.protocol.dto.FlowMetaDto
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.union.core.exception.UnionMetaException
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.flow.converter.FlowItemConverter
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
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
        try {
            val royalties = flowNftItemControllerApi.getNftItemRoyaltyById(itemId).awaitFirst()
            return royalties.royalty.map { FlowItemConverter.toRoyalty(it, blockchain) }
        } catch (e: WebClientResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return emptyList()
            }
            throw e
        }
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        val meta = flowNftItemControllerApi.getNftItemMetaById(itemId).awaitFirst()

        return when (meta.status) {
            FlowMetaDto.Status.CORRUPTED_URL -> throw UnionMetaException(
                UnionMetaException.ErrorCode.CORRUPTED_URL,
                "Can't parse meta url for: $itemId"
            )

            FlowMetaDto.Status.CORRUPTED_DATA -> throw UnionMetaException(
                UnionMetaException.ErrorCode.CORRUPTED_DATA,
                "Can't parse meta json for: $itemId"
            )

            FlowMetaDto.Status.TIMEOUT -> throw UnionMetaException(
                UnionMetaException.ErrorCode.TIMEOUT,
                "Timeout during loading meta for: $itemId"
            )

            FlowMetaDto.Status.ERROR -> throw UnionMetaException(
                UnionMetaException.ErrorCode.ERROR,
                message = null
            )

            FlowMetaDto.Status.NOT_FOUND -> throw UnionMetaException(
                UnionMetaException.ErrorCode.NOT_FOUND,
                message = null
            )

            FlowMetaDto.Status.OK, null -> FlowItemConverter.convert(meta, itemId)
        }

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

    override suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        val items = flowNftItemControllerApi.getItemByIds(FlowItemIdsDto(itemIds)).awaitSingle().items
        return items.map { FlowItemConverter.convert(it, blockchain) }
    }

    override suspend fun getItemCollectionId(itemId: String): String {
        // TODO is validation possible here?
        return itemId.substringBefore(":")
    }

}
