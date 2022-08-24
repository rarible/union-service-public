package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionItemMetaRefreshEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.converter.ImxItemConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImxItemMetaConverter
import com.rarible.protocol.union.integration.immutablex.model.ImxItemMeta
import com.rarible.protocol.union.integration.immutablex.repository.ImxItemMetaRepository
import com.rarible.protocol.union.integration.immutablex.service.ImxItemService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ImxItemEventHandler(
    private val itemMetaHandler: IncomingEventHandler<UnionItemMetaEvent>,
    private val itemHandler: IncomingEventHandler<UnionItemEvent>,
    private val itemService: ImxItemService,
    private val itemMetaRepository: ImxItemMetaRepository
) {

    private val blockchain = BlockchainDto.IMMUTABLEX

    suspend fun handle(assets: List<ImmutablexAsset>) = coroutineScope {
        if (assets.isEmpty()) {
            return@coroutineScope
        }
        val itemIds = assets.filter { !it.isDeleted() }.map { it.itemId }

        val creatorsDeferred = async { itemService.getItemCreators(itemIds) }
        val metaAttributesDeferred = async { itemService.getMetaAttributeKeys(itemIds) }

        val currentMeta = getLastItemMeta(itemIds)
        val creators = creatorsDeferred.await()
        val metaAttributes = metaAttributesDeferred.await()

        val meta = assets.associateBy { it.itemId }.mapValues {
            ImxItemMetaConverter.convert(it.value, metaAttributes[it.key], blockchain)
        }

        assets.forEach { asset ->
            val assetId = asset.itemId
            val item = ImxItemConverter.convert(asset, creators[assetId], blockchain)
            if (item.deleted) {
                itemHandler.onEvent(UnionItemDeleteEvent(item.id))
            } else {
                val newMeta = meta[assetId]!!
                val oldMeta = currentMeta[assetId]
                val metaChanged = newMeta != oldMeta

                if (oldMeta == null || metaChanged) {
                    itemMetaRepository.save(ImxItemMeta(assetId, newMeta))
                }

                // Send refresh task ONLY if there is old meta, and it has been changed
                // Otherwise, listener should trigger update itself
                if (oldMeta != null && metaChanged) {
                    itemMetaHandler.onEvent(UnionItemMetaRefreshEvent(item.id))
                }
                itemHandler.onEvent(UnionItemUpdateEvent(item))
            }
        }
    }

    private suspend fun getLastItemMeta(assetIds: Collection<String>): Map<String, UnionMeta> {
        if (assetIds.isEmpty()) {
            return emptyMap()
        }
        return itemMetaRepository.getAll(assetIds).associateBy({ it.id }, { it.meta })
    }
}
