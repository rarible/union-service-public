package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.continuation.UnionOwnershipContinuation
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.client.ImxAssetClient
import com.rarible.protocol.union.integration.immutablex.client.TokenIdDecoder
import com.rarible.protocol.union.integration.immutablex.converter.ImxOwnershipConverter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException

class ImxOwnershipService(
    private val assetClient: ImxAssetClient,
    private val itemService: ImxItemService
) : AbstractBlockchainService(BlockchainDto.IMMUTABLEX), OwnershipService {

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val itemId = TokenIdDecoder.decodeItemId(ownershipId.substringBeforeLast(":"))
        val owner = ownershipId.substringAfterLast(":")
        val asset = assetClient.getById(itemId)

        if (asset.user == owner) {
            val creator = itemService.getItemCreator(itemId)
            return ImxOwnershipConverter.convert(asset, creator, blockchain)
        } else {
            // Enrichment uses such kind of exceptions in order to skip events for non-existing ownerships
            throw WebClientResponseProxyException(
                WebClientResponseException(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.reasonPhrase,
                    null,
                    null,
                    null,
                )
            )
        }
    }

    override suspend fun getOwnershipsByIds(ownershipIds: List<String>): List<UnionOwnership> {
        val itemIds = ownershipIds.map { TokenIdDecoder.decodeItemId(it.substringBeforeLast(":")) }.toSet()

        val creatorsDeferred = coroutineScope { async { itemService.getItemCreators(itemIds) } }
        val assets = assetClient.getByIds(itemIds)

        val ownershipIdsSet = HashSet(ownershipIds)
        val found = assets.filter {
            ownershipIdsSet.contains("${it.itemId}:${it.user}")
        }

        val creators = creatorsDeferred.await()
        return assets.map { ImxOwnershipConverter.convert(it, creators[it.itemId], blockchain) }
    }

    override suspend fun getOwnershipsAll(continuation: String?, size: Int): Slice<UnionOwnership> {
        val itemContinuation = toItemContinuation(continuation)
        val page = assetClient.getAllAssets(itemContinuation, size, null, null, false)
        val converted = convert(page.result)
        return Paging(UnionOwnershipContinuation.ByLastUpdatedAndId, converted).getSlice(size)
    }

    override suspend fun getOwnershipsByItem(itemId: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val decodedItemId = TokenIdDecoder.decodeItemId(itemId)
        val creator = coroutineScope { async { itemService.getItemCreator(decodedItemId) } }
        val asset = assetClient.getById(decodedItemId)
        val result = ImxOwnershipConverter.convert(asset, creator.await(), blockchain)
        return Page(0L, null, listOf(result))
    }

    override suspend fun getOwnershipsByOwner(address: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val itemContinuation = toItemContinuation(continuation)
        val assets = assetClient.getAssetsByOwner(address, itemContinuation, size).result
        val result = convert(assets)
        return Paging(UnionOwnershipContinuation.ByLastUpdatedAndId, result).getPage(size, 0)
    }

    private suspend fun convert(assets: Collection<ImmutablexAsset>): List<UnionOwnership> {
        val creators = itemService.getItemCreators(assets.map { it.itemId })
        return assets.map { ImxOwnershipConverter.convert(it, creators[it.itemId], blockchain) }
    }

    private fun toItemContinuation(continuation: String?): String? {
        return DateIdContinuation.parse(continuation)?.let {
            val itemId = OwnershipIdParser.parseShort(it.id, blockchain).itemIdValue
            DateIdContinuation(it.date, itemId).toString()
        }
    }
}
