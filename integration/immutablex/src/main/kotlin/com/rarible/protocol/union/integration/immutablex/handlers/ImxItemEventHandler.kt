package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionItemMetaUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.converter.ImxItemConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImxItemMetaConverter
import com.rarible.protocol.union.integration.immutablex.service.ImxItemService

class ImxItemEventHandler(
    private val itemMetaHandler: IncomingEventHandler<UnionItemMetaEvent>,
    private val itemService: ImxItemService
) {

    private val blockchain = BlockchainDto.IMMUTABLEX

    suspend fun handle(assets: List<ImmutablexAsset>) {
        if (assets.isEmpty()) {
            return
        }
        val creators = itemService.getItemCreators(assets.map { it.itemId })
        val items = ImxItemConverter.convert(assets, creators, blockchain)
        val meta = assets.associateBy { it.encodedItemId() }
            .mapValues { ImxItemMetaConverter.convert(it.value, blockchain) }

        items.forEach {
            itemMetaHandler.onEvent(UnionItemMetaUpdateEvent(it.id, it, meta[it.id.value]!!))
        }
    }
}
